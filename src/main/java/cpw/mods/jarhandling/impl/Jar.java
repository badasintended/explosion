// @formatter:off

/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package cpw.mods.jarhandling.impl;

import cpw.mods.jarhandling.JarMetadata;
import cpw.mods.jarhandling.SecureJar;
import cpw.mods.niofs.union.UnionFileSystemProvider;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.module.ModuleDescriptor;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.spi.FileSystemProvider;
import java.security.CodeSigner;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

public class Jar implements SecureJar {
    private static final CodeSigner[] EMPTY_CODESIGNERS = new CodeSigner[0];
    private static final FileSystemProvider UFSP = new UnionFileSystemProvider();
    private final Manifest manifest;
    private final Hashtable<String, CodeSigner[]> pendingSigners = new Hashtable<>();
    private final Hashtable<String, CodeSigner[]> verifiedSigners = new Hashtable<>();
    private final ManifestVerifier verifier = new ManifestVerifier();
    private final Map<String, StatusData> statusData = new HashMap<>();
    private final JarMetadata metadata;
    private final Path filesystemRoot;
    private final Path filesystemPrimary;
    private final Map<String, String> nameOverrides;
    private final JarModuleDataProvider moduleDataProvider;
    private final Set<String> packages;
    private final List<Provider> providers;

    @Override
    public ModuleDataProvider moduleDataProvider() {
        return moduleDataProvider;
    }

    @Override
    public Path getPrimaryPath() {
        return filesystemPrimary;
    }

    public Jar(Function<SecureJar, JarMetadata> metadataFunction, BiPredicate<String, String> pathfilter, Path... paths) {
        this(null, metadataFunction, pathfilter, paths);
    }

    /** Supplying a manifest is stupid. */
    @Deprecated(forRemoval = true, since = "2.2")
    public Jar(Supplier<Manifest> defaultManifest, Function<SecureJar, JarMetadata> metadataFunction, BiPredicate<String, String> pathfilter, Path... paths) {
        var validPaths = Arrays.stream(paths)
            .map(Path::toAbsolutePath)
            .map(Path::normalize)
            .filter(Files::exists)
            .toArray(Path[]::new);

        if (validPaths.length == 0)
            throw new UncheckedIOException(new IOException("Invalid paths argument, contained no existing paths: " + Arrays.toString(paths)));
        this.moduleDataProvider = new JarModuleDataProvider(this);

        this.filesystemRoot = newFileSystem(pathfilter, validPaths);
        this.filesystemPrimary = validPaths[validPaths.length - 1];
        this.manifest = findManifest(validPaths, defaultManifest);
        this.nameOverrides = gatherVersionedFiles();
        this.providers = gatherProviders(pathfilter);
        this.packages = gatherPackages();
        this.metadata = metadataFunction.apply(this);
    }

    @Override
    public CodeSigner[] getManifestSigners() {
        return getData(JarFile.MANIFEST_NAME).map(r->r.signers).orElse(null);
    }

    @Override
    public Status verifyPath(Path path) {
        if (path.getFileSystem() != this.filesystemRoot.getFileSystem())
            throw new IllegalArgumentException("Wrong filesystem");

        final var pathname = path.toString();
        if (statusData.containsKey(pathname))
            return getFileStatus(pathname);

        try {
            var bytes = Files.readAllBytes(path);
            verifyAndGetSigners(pathname, bytes);
            return getFileStatus(pathname);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public Status getFileStatus(final String name) {
        return hasSecurityData() ? getData(name).map(StatusData::status).orElse(Status.NONE) : Status.UNVERIFIED;
    }

    @Override
    public Attributes getTrustedManifestEntries(final String name) {
        var manattrs = manifest.getAttributes(name);
        var mansigners = getManifestSigners();
        var objsigners = getData(name).map(sd->sd.signers).orElse(EMPTY_CODESIGNERS);
        if (mansigners == null || (mansigners.length == objsigners.length)) {
            return manattrs;
        } else {
            return null;
        }
    }

    @Override
    public boolean hasSecurityData() {
        return !pendingSigners.isEmpty() || !this.verifiedSigners.isEmpty();
    }

    @Override
    public String name() {
        return metadata.name();
    }

    @Override
    public Set<String> getPackages() {
        return this.packages;
    }

    @Override
    public List<Provider> getProviders() {
        return this.providers;
    }

    @Override
    public Path getPath(String first, String... rest) {
        var rel = this.filesystemRoot.getFileSystem().getPath(first, rest);
        return this.filesystemRoot.resolve(rel);
    }

    @Override
    public Path getRootPath() {
        return this.filesystemRoot;
    }

    @Override
    public String toString() {
        return "Jar[" + getURI() + "]";
    }

    /*==============================================================================================*
     *                                INTERNAL SHIT                                                 *
     *==============================================================================================*/

    private Path newFileSystem(BiPredicate<String, String> filter, Path[] paths) {
        if (paths == null || paths.length == 0)
            throw new IllegalArgumentException("Must contain atleast one path");

        FileSystem fs = null;
        try {
            if (filter == null && paths.length == 1) {
                if (Files.isDirectory(paths[0]))
                    return paths[0];
                var uri = paths[0].toUri();
                if ("file".equals(uri.getScheme())) {
                    // We have to manually open the jar files up via a URI instead of a Path
                    // because the ZipFileSystem implementation only caches the FileSystems
                    // when accessed that way. But we can only open it once or else it throws
                    // a FileSystemAlreadyExistsException. So, exceptions as codeflow, yay!
                    uri = new URI("jar:" + uri);
                    try {
                        fs = FileSystems.newFileSystem(uri, Map.of(), null);
                    } catch (FileSystemAlreadyExistsException e) {
                        fs = FileSystems.getFileSystem(uri);
                    }
                } else {
                    // JarInJar is fucking stupid and breaks horribly if it knows about files directly.
                    // So because I don't want to go into the rabbit hole that is digging into that
                    // Any non-standard file system will be wrapped in a Union File System
                    // This still gets the performance benefit of having 90% of everything bypass the UFS
                    // TODO: [SM] Remove JarInJar file system in favor of a simpler dependency management system.
                    fs = UFSP.newFileSystem(paths[0], Map.of("filter", (BiPredicate<String, String>)(a, b) -> true));
                }
            } else {
                var map = new HashMap<String, Object>();
                if (filter != null)
                    map.put("filter", filter);

                var lst = new ArrayList<>(Arrays.asList(paths));
                var base = lst.remove(0);
                map.put("additional", lst);

                fs = UFSP.newFileSystem(base, map);
            }
        } catch (IOException | URISyntaxException e) {
            return sneak(e);
        }

        return fs.getRootDirectories().iterator().next();
    }

    /** Public for API compat, will break soon-ish */
    public synchronized CodeSigner[] verifyAndGetSigners(String name, byte[] bytes) {
        if (!hasSecurityData())
            return null;

        var data = statusData.get(name);
        if (data != null)
            return data.signers();

        var signers = verifier.verify(this.manifest, pendingSigners, verifiedSigners, name, bytes);
        if (signers == null) {
            this.statusData.put(name, new StatusData(Status.INVALID, null));
            return null;
        } else {
            var ret = signers.orElse(null);
            this.statusData.put(name, new StatusData(Status.VERIFIED, ret));
            return ret;
        }
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable, R> R sneak(Throwable e) throws E {
        throw (E)e;
    }

    private Optional<StatusData> getData(final String name) {
        return Optional.ofNullable(statusData.get(name));
    }

    public Manifest getManifest() {
        return manifest;
    }

    public URI getURI() {
        return this.filesystemRoot.toUri();
    }

    public ModuleDescriptor computeDescriptor() {
        return metadata.descriptor();
    }

    public Optional<URI> findFile(String name) {
        name = nameOverrides.getOrDefault(name, name);
        var resolved = filesystemRoot.resolve(name);
        if (Files.exists(resolved))
            return Optional.of(resolved.toUri());
        return Optional.empty();
    }

    private record StatusData(Status status, CodeSigner[] signers) {}

    private List<Provider> gatherProviders(BiPredicate<String, String> filter) {
        var services = this.filesystemRoot.resolve("META-INF/services/");
        if (!Files.exists(services))
            return List.of();

        try {
            return Files.walk(services)
                .filter(Files::isRegularFile)
                .map(path -> getProvider(path, filter))
                .toList();
        } catch (IOException e) {
            return sneak(e);
        }
    }

    /* Public for SecureJar only */
    public static Provider getProvider(Path path, BiPredicate<String, String> filter) {
        var sname = path.getFileName().toString();
        try {
            var entries = Files.readAllLines(path).stream()
                .map(String::trim)
                .filter(l -> l.length() > 0 && !l.startsWith("#"))
                .filter(p -> filter == null || filter.test(p.replace('.', '/'), ""))
                .toList();
            return new Provider(sname, entries);
        } catch (IOException e) {
            return sneak(e);
        }
    }

    private Map<String, String> gatherVersionedFiles() {
        var versionsDir = this.filesystemRoot.resolve("META-INF/versions");
        if (!Boolean.parseBoolean(getManifest().getMainAttributes().getValue("Multi-Release")) || !Files.exists(versionsDir))
            return Map.of();

        var ret = new HashMap<String, String>();
        var versions = new HashMap<String, Integer>();
        try {
            Files.walk(versionsDir)
                .filter(Files::isRegularFile)
                .map(filesystemRoot::relativize)
                .forEach(path -> {
                    var ver = Integer.parseInt(path.getName(2).toString());
                    var key = path.subpath(3, path.getNameCount()).toString().replace('\\', '/');
                    if (ver <= Runtime.version().feature() && versions.getOrDefault(key, 0) < ver) {
                        versions.put(key, ver);
                        ret.put(key, path.toString());
                    }
                });
        } catch (IOException e) {
            sneak(e);
        }
        return ret;
    }

    private Set<String> gatherPackages() {
        var files = new HashSet<String>(this.nameOverrides.keySet());
        try {
            Files.walk(this.filesystemRoot)
                .filter(p -> Files.isRegularFile(p) && !"META-INF".equals(p.getName(0).toString()))
                .map(p -> this.filesystemRoot.relativize(p).toString().replace('\\', '/'))
                .forEach(files::add);
        } catch (IOException e) {
            return sneak(e);
        }

        var ret = new HashSet<String>();
        for (var file : files) {
            int idx = file.lastIndexOf('/');
            if (!file.endsWith(".class") || idx == -1)
                continue;
            ret.add(file.substring(0, idx).replace('/', '.'));
        }
        return ret;
    }

    private Manifest findManifest(Path[] paths, Supplier<Manifest> defaultManifest) {
        try {
            for (int x = paths.length - 1; x >= 0; x--) { // Walk backwards because this is what cpw wanted?
                var path = paths[x];
                if (Files.isDirectory(path)) {
                    var manfile = path.resolve(JarFile.MANIFEST_NAME);
                    if (Files.exists(manfile)) {
                        try (var is = Files.newInputStream(manfile)) {
                            return new Manifest(is);
                        }
                    }
                } else {
                    try (var jis = new JarInputStream(Files.newInputStream(path))) {
                        var jv = SecureJarVerifier.getJarVerifier(jis);
                        if (jv != null) {
                            // Read till we find the manifest, or run out of entries.
                            while (SecureJarVerifier.isParsingMeta(jv)) {
                                if (jis.getNextJarEntry() == null)
                                    break;
                            }

                            if (SecureJarVerifier.hasSignatures(jv)) {
                                pendingSigners.putAll(SecureJarVerifier.getPendingSigners(jv));
                                var manifestSigners = SecureJarVerifier.getVerifiedSigners(jv).get(JarFile.MANIFEST_NAME);
                                if (manifestSigners != null)
                                    verifiedSigners.put(JarFile.MANIFEST_NAME, manifestSigners);
                                this.statusData.put(JarFile.MANIFEST_NAME, new StatusData(Status.VERIFIED, manifestSigners));
                            }
                        }

                        if (jis.getManifest() != null)
                            return new Manifest(jis.getManifest());
                    }
                }
            }
            return defaultManifest != null ? defaultManifest.get() : new Manifest();
        } catch (IOException e) {
            return sneak(e);
        }
    }

    private record JarModuleDataProvider(Jar jar) implements ModuleDataProvider {
        @Override
        public String name() {
            return jar.name();
        }

        @Override
        public ModuleDescriptor descriptor() {
            return jar.computeDescriptor();
        }

        @Override
        public URI uri() {
            return jar.getURI();
        }

        @Override
        public Optional<URI> findFile(final String name) {
            return jar.findFile(name);
        }

        @Override
        public Optional<InputStream> open(String name) {
            // Path.toURI() can sometimes return URIs that are invalid syntax/can't be passed to Paths.get
            // Specifically ZipPath and jars with []'s. https://github.com/MinecraftForge/MinecraftForge/issues/9842
            // So bypass all of that and get the InputStream from the path itself.
            name = jar.nameOverrides.getOrDefault(name, name);
            var resolved = jar.filesystemRoot.resolve(name);
            if (Files.exists(resolved)) {
                try {
                    return Optional.of(Files.newInputStream(resolved));
                } catch (IOException e) {
                    return sneak(e);
                }
            }
            return Optional.empty();
        }

        @Override
        public Manifest getManifest() {
            return jar.getManifest();
        }

        @Override
        public CodeSigner[] verifyAndGetSigners(final String cname, final byte[] bytes) {
            return jar.verifyAndGetSigners(cname, bytes);
        }
    }
}
