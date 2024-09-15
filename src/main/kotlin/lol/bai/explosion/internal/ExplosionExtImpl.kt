package lol.bai.explosion.internal

import com.google.common.hash.Hashing
import lol.bai.explosion.ExplosionDesc
import lol.bai.explosion.ExplosionExt
import lol.bai.explosion.internal.fabric.FakeGameProvider
import net.fabricmc.api.EnvType
import net.fabricmc.loader.impl.FabricLoaderImpl
import net.fabricmc.loader.impl.discovery.ModResolver
import net.fabricmc.loader.impl.discovery.createModDiscoverer
import net.fabricmc.loader.impl.launch.FabricLauncherBase
import net.fabricmc.loader.impl.launch.knot.Knot
import net.minecraftforge.fml.loading.UniqueModListBuilder
import net.minecraftforge.fml.loading.moddiscovery.JarInJarDependencyLocator
import net.minecraftforge.fml.loading.moddiscovery.ModFile
import net.minecraftforge.fml.loading.moddiscovery.createModsFolderLocator
import net.minecraftforge.forgespi.locating.IModFile
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.Transformer
import org.gradle.kotlin.dsl.invoke
import java.io.File
import java.nio.file.Path
import kotlin.io.path.*

private class BomDependency(
    val group: String,
    val name: String,
    val version: String
)

@Suppress("UnstableApiUsage")
open class ExplosionExtImpl(
    private val project: Project,
    private val outputDir: Path,
    private val transformerId: String?,
    private val transformer: Transformer<Path, Path>?
) : ExplosionExt {

    @Suppress("unused")
    constructor(project: Project, outputDir: Path) : this(project, outputDir, null, null)

    private fun Path.resolve(vararg path: String): Path {
        return resolve(path.joinToString(File.separator))
    }

    private fun group(platform: String?): String {
        return if (platform == null) "exploded" else "exploded.${platform}"
    }

    private fun createPom(platform: String?, name: String, version: String, jarPlacer: (Path) -> Unit): BomDependency {
        val group = group(platform)

        var sanitizedVersion = version.replace(Regex("[^A-Za-z0-9.]"), "_")
        if (transformerId != null) sanitizedVersion = "${sanitizedVersion}_transformed_$transformerId"

        val pom = this.javaClass.classLoader.getResource("artifact.xml")!!.readText()
            .replace("%GROUP_ID%", group)
            .replace("%ARTIFACT_ID%", name)
            .replace("%VERSION%", sanitizedVersion)

        val dir = when (platform) {
            null -> outputDir.resolve("exploded", name, sanitizedVersion)
            else -> outputDir.resolve("exploded", platform, name, sanitizedVersion)
        }

        dir.createDirectories()
        dir.resolve("${name}-${sanitizedVersion}.pom").writeText(pom)

        val jarPath = dir.resolve("${name}-${sanitizedVersion}.jar")
        jarPlacer(jarPath)

        if (transformer != null) {
            val originalJarPath = dir.resolve("__original-${transformerId}.jar")
            jarPath.moveTo(originalJarPath, overwrite = true)

            val transformed = transformer.transform(originalJarPath)
            transformed.moveTo(jarPath, overwrite = true)

            originalJarPath.deleteIfExists()
        }

        return BomDependency(group, name, sanitizedVersion)
    }

    private fun getOrCreateBom(hash: String, deps: () -> List<BomDependency>): String {
        val bom = "exploded-bom:${hash}:1"
        val dir = outputDir.resolve("exploded-bom", hash, "1")
        dir.createDirectories()
        val output = dir.resolve("${hash}-1.pom")

        if (output.exists()) {
            project.logger.lifecycle("Exploded BOM for hash $hash already exists, skipping")
            return bom
        }

        project.logger.lifecycle("Building exploded BOM for hash $hash")

        val depTemplate = this.javaClass.classLoader.getResource("bom_dependency.xml")!!.readText()

        val depsStr = StringBuilder()
        deps().forEach {
            depsStr.append('\n')

            val lines = depTemplate
                .replace("%GROUP_ID%", it.group)
                .replace("%ARTIFACT_ID%", it.name)
                .replace("%VERSION%", it.version)
                .lines()
            depsStr.append(lines.joinToString(separator = "\n        ", prefix = "        "))
        }
        depsStr.append('\n')

        val pom = this.javaClass.classLoader.getResource("bom_root.xml")!!.readText()
            .replace("%GROUP_ID%", "exploded-bom")
            .replace("%ARTIFACT_ID%", hash)
            .replace("%VERSION%", "1")
            .replace("%DEPENDENCIES%", depsStr.toString())

        output.writeText(pom)
        return bom
    }

    private fun resolve(
        action: Action<ExplosionDesc>,
        resolver: (Path) -> List<BomDependency>
    ) = project.provider {
        val desc = ExplosionDescImpl(project)
        action(desc)

        val tempDir = createTempDirectory()

        try {
            val hashBuilder = StringBuilder()
            if (transformerId != null) hashBuilder.append(transformerId)

            desc.resolveJars {
                hashBuilder.append(Hashing.murmur3_128().hashBytes(it.readBytes()))
                hashBuilder.append(";")

                it.copyTo(tempDir.resolve(it.name).toFile())
            }

            val hash = Hashing.murmur3_128().hashString(hashBuilder.toString(), Charsets.UTF_8).toString()

            return@provider getOrCreateBom(hash) { resolver(tempDir) }
        } finally {
            tempDir.toFile().deleteRecursively()
        }
    }

    override fun withTransformer(id: String, transformer: Transformer<Path, Path>): ExplosionExt {
        return ExplosionExtImpl(project, outputDir, id, transformer)
    }

    override fun fabric(action: Action<ExplosionDesc>) = resolve(action) { tempDir ->
        if (FabricLauncherBase.getLauncher() == null) Knot(EnvType.CLIENT)

        val loader = FabricLoaderImpl.INSTANCE.apply {
            gameProvider = FakeGameProvider(tempDir)
        }

        val candidates = createModDiscoverer(tempDir).discoverMods(loader, mutableMapOf())
        candidates.removeIf { it.id == "java" }

        val candidateIds = hashSetOf<String>()

        candidates.forEach {
            candidateIds.add(it.id)
            candidateIds.addAll(it.provides)
        }

        candidates.forEach { candidate ->
            candidate.metadata.dependencies = candidate.metadata.dependencies.filter {
                candidateIds.contains(it.modId)
            }
        }

        val mods = ModResolver.resolve(candidates, EnvType.CLIENT, mutableMapOf())
        val bomDeps = arrayListOf<BomDependency>()

        for (mod in mods) {
            bomDeps.add(createPom(null, mod.id, mod.version.friendlyString) { path ->
                mod.copyToDir(path.parent, false).moveTo(path, overwrite = true)
            })
        }

        return@resolve bomDeps
    }

    override fun forge(action: Action<ExplosionDesc>) = resolve(action) { tempDir ->
        val modLocator = createModsFolderLocator(tempDir, "explosion!!!")
        val jarJar = JarInJarDependencyLocator()

        val primeModFiles = modLocator.scanMods().map { it.file }
        val jarJarModFiles = jarJar.scanMods(primeModFiles)
        val combinedModFiles = (primeModFiles + jarJarModFiles).filterIsInstance<ModFile>()

        val uniqueModFiles = UniqueModListBuilder(combinedModFiles).buildUniqueList().modFiles
            .filter { it.type == IModFile.Type.MOD }

        val bomDeps = arrayListOf<BomDependency>()

        for (modFile in uniqueModFiles) {
            val mod = modFile.modInfos[0]

            bomDeps.add(createPom("__forge", mod.modId, mod.version.toString()) { path ->
                modFile.filePath.copyTo(path, overwrite = true)
            })
        }

        return@resolve bomDeps
    }

}