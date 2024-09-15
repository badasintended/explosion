package lol.bai.explosion.internal

import com.google.common.hash.Hashing
import lol.bai.explosion.ExplosionDesc
import lol.bai.explosion.ExplosionExt
import lol.bai.explosion.internal.resolver.ResolverTask
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.Transformer
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.invoke
import java.io.File
import java.nio.file.Path
import kotlin.io.path.*

private class BomDependency(
    val group: String,
    val name: String,
    val version: String
)

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

    private fun createPom(loader: String, name: String, version: String, jarPlacer: (Path) -> Unit): BomDependency {
        val group = "exploded"

        var sanitizedVersion = loader + "_" + version.replace(Regex("[^A-Za-z0-9.]"), "_")
        if (transformerId != null) sanitizedVersion = "${sanitizedVersion}_transformed_$transformerId"

        val pom = this.javaClass.classLoader.getResource("artifact.xml")!!.readText()
            .replace("%GROUP_ID%", group)
            .replace("%ARTIFACT_ID%", name)
            .replace("%VERSION%", sanitizedVersion)

        val dir = outputDir.resolve("exploded", name, sanitizedVersion)

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
        loader: String
    ) = project.provider {
        val desc = ExplosionDescImpl(project)
        action(desc)

        val inputDir = createTempDirectory()
        val outputDir = createTempDirectory()

        try {
            val hashBuilder = StringBuilder(loader).append(";")
            if (transformerId != null) hashBuilder.append(transformerId).append(";")

            desc.resolveJars {
                hashBuilder.append(Hashing.murmur3_128().hashBytes(it.readBytes()))
                hashBuilder.append(";")

                it.copyTo(inputDir.resolve(it.name).toFile())
            }

            val hash = Hashing.murmur3_128().hashString(hashBuilder.toString(), Charsets.UTF_8).toString()

            return@provider getOrCreateBom(hash) {
                val task = project.tasks.create<ResolverTask>("__explosion_resolver_" + Any().hashCode()) {
                    this.loader.set(loader)
                    this.inputDir.set(inputDir.toFile())
                    this.outputDir.set(outputDir.toFile())
                }

                task.exec()
                task.enabled = false
                val metaLines = outputDir.resolve("__meta.txt").readLines()
                val bomDeps = arrayListOf<BomDependency>()

                for (line in metaLines) {
                    val trimmed = line.trim()
                    if (trimmed.isEmpty()) continue

                    val (modFile, modId, version) = trimmed.split("\t")
                    bomDeps.add(createPom(loader, modId, version) { path ->
                        outputDir.resolve(modFile).copyTo(path)
                    })
                }

                return@getOrCreateBom bomDeps
            }
        } finally {
            inputDir.toFile().deleteRecursively()
            outputDir.toFile().deleteRecursively()
        }
    }

    override fun withTransformer(id: String, transformer: Transformer<Path, Path>): ExplosionExt {
        return ExplosionExtImpl(project, outputDir, id, transformer)
    }

    override fun fabric(action: Action<ExplosionDesc>) = resolve(action, "fabric")
    override fun forge(action: Action<ExplosionDesc>) = resolve(action, "forge")

}