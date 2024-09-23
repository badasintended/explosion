package lol.bai.explosion.gradle.internal

import com.google.common.hash.Hashing
import lol.bai.explosion.gradle.ExplosionPlatform
import lol.bai.explosion.gradle.ExplosionPlatformConfig
import lol.bai.explosion.gradle.ExplosionPlatformDesc
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.embeddedKotlin
import org.gradle.kotlin.dsl.invoke
import java.io.File
import java.nio.file.Path
import kotlin.io.path.*

private class BomDependency(
    val group: String,
    val name: String,
    val version: String
)

class Platform(
    private val project: Project,
    private val loader: String,
    private val outputDir: Path,
    private val config: PlatformConfig?
) : ExplosionPlatform {

    private fun Path.resolve(vararg path: String): Path {
        return resolve(path.joinToString(File.separator))
    }

    private fun log(message: String) {
        project.logger.lifecycle("lol.bai.explosion: $message")
    }

    private fun createPom(loader: String, name: String, version: String, jarPlacer: (Path) -> Unit): BomDependency {
        val group = "exploded"

        var sanitizedVersion = loader + "_" + version.replace(Regex("[^A-Za-z0-9.]"), "_")
        if (config != null) sanitizedVersion = "${sanitizedVersion}_transformed_${config.id}"

        val pom = this.javaClass.classLoader.getResource("artifact.xml")!!.readText()
            .replace("%GROUP_ID%", group)
            .replace("%ARTIFACT_ID%", name)
            .replace("%VERSION%", sanitizedVersion)

        val dir = outputDir.resolve("exploded", name, sanitizedVersion)

        dir.createDirectories()
        dir.resolve("${name}-${sanitizedVersion}.pom").writeText(pom)

        val jarPath = dir.resolve("${name}-${sanitizedVersion}.jar")
        jarPlacer(jarPath)

        if (config?.transformer != null) {
            val originalJarPath = dir.resolve("__original-${config.id}.jar")
            jarPath.moveTo(originalJarPath, overwrite = true)

            val transformed = config.transformer!!.transform(originalJarPath)
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
            log("Exploded BOM for hash $hash already exists, skipping")
            return bom
        }

        log("Building exploded BOM for hash $hash")

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

        log("Done")
        return bom
    }

    override fun invoke(action: Action<ExplosionPlatformDesc>) = project.provider {
        val desc = PlatformDesc(project)
        action(desc)

        val inputDir = createTempDirectory()
        val outputDir = createTempDirectory()

        try {
            val hashBuilder = StringBuilder(loader).append(";")
            if (config != null) hashBuilder.append(config.id).append(";")

            desc.resolveJars {
                hashBuilder.append(Hashing.murmur3_128().hashBytes(it.readBytes()))
                hashBuilder.append(";")

                it.copyTo(inputDir.resolve(it.name).toFile(), overwrite = true)
            }

            val hash = Hashing.murmur3_128().hashString(hashBuilder.toString(), Charsets.UTF_8).toString()

            return@provider getOrCreateBom(hash) {
                val key = "__explosion_resolver_" + Any().hashCode()
                val meta = javaClass.classLoader.getResource("__meta.txt")!!.readText().trim()
                val (group, _, version) = meta.split(':')
                val configuration = project.configurations.create(key)

                project.dependencies {
                    configuration(embeddedKotlin("stdlib"))
                    configuration(group, "explosion-resolver-${loader}", version)
                }

                val task = project.tasks.create<ResolverTask>(key) {
                    this.configuration.set(key)
                    this.mainClass.set("lol.bai.explosion.resolver.${loader}.MainKt")
                    this.inputDir.set(inputDir.toFile())
                    this.outputDir.set(outputDir.toFile())
                }

                task.exec()
                task.enabled = false
                project.configurations.remove(configuration)

                val bomDeps = arrayListOf<BomDependency>()
                outputDir.resolve("__meta.txt").forEachLine { line ->
                    log(line)
                    val trimmed = line.trim()
                    if (trimmed.isNotEmpty()) {
                        val (modFile, modId, modVersion) = trimmed.split("\t")
                        bomDeps.add(createPom(loader, modId, modVersion) { path ->
                            outputDir.resolve(modFile).copyTo(path, overwrite = true)
                        })
                    }
                }

                return@getOrCreateBom bomDeps
            }
        } finally {
            inputDir.toFile().deleteRecursively()
            outputDir.toFile().deleteRecursively()
        }
    }

    override fun with(id: String, action: Action<ExplosionPlatformConfig>): ExplosionPlatform {
        val config = PlatformConfig(id)
        action(config)
        return Platform(project, loader, outputDir, config)
    }

}