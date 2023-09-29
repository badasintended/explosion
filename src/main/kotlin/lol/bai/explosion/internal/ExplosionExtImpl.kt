package lol.bai.explosion.internal

import lol.bai.explosion.ExplosionDesc
import lol.bai.explosion.ExplosionExt
import lol.bai.explosion.internal.fabric.FakeGameProvider
import net.fabricmc.api.EnvType
import net.fabricmc.loader.impl.FabricLoaderImpl
import net.fabricmc.loader.impl.discovery.ModResolver
import net.fabricmc.loader.impl.discovery.createModDiscoverer
import net.fabricmc.loader.impl.launch.FabricLauncherBase
import net.fabricmc.loader.impl.launch.knot.Knot
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.kotlin.dsl.invoke
import java.io.File
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.createTempDirectory
import kotlin.io.path.moveTo
import kotlin.io.path.writeText

abstract class ExplosionExtImpl(
    private val project: Project,
    private val outputDir: Path
) : ExplosionExt {

    init {
        outputDir.createDirectories()
    }

    private fun Path.resolve(vararg path: String): Path {
        return resolve(path.joinToString(File.separator))
    }

    private fun sanitizeVersion(version: String): String {
        return version.replace(Regex("[^A-Za-z0-9.]"), "_")
    }

    private fun createPom(name: String, version: String): Path {
        val pom = this.javaClass.classLoader.getResource("artifact.xml")!!.readText()
            .replace("%GROUP_ID%", "exploded")
            .replace("%ARTIFACT_ID%", name)
            .replace("%VERSION%", version)

        val dir = outputDir.resolve("exploded", name, version)
        dir.createDirectories()
        dir.resolve("${name}-${version}.pom").writeText(pom)
        return dir.resolve("${name}-${version}.jar")
    }

    private fun createBom(hash: String, deps: List<Pair<String, String>>): String {
        val depTemplate = this.javaClass.classLoader.getResource("bom_dependency.xml")!!.readText()

        fun createDependency(group: String, name: String, version: String): String {
            val lines = depTemplate.replace("%GROUP_ID%", group)
                .replace("%ARTIFACT_ID%", name)
                .replace("%VERSION%", version)
                .lines()

            return lines.joinToString(separator = "\n        ", prefix = "        ")
        }

        val depsStr = StringBuilder()
        deps.forEach { (depName, depVersion) ->
            depsStr.append('\n')
            depsStr.append(createDependency("exploded", depName, depVersion))
        }
        depsStr.append('\n')

        val pom = this.javaClass.classLoader.getResource("bom_root.xml")!!.readText()
            .replace("%GROUP_ID%", "exploded-bom")
            .replace("%ARTIFACT_ID%", hash)
            .replace("%VERSION%", "1")
            .replace("%DEPENDENCIES%", depsStr.toString())

        val dir = outputDir.resolve("exploded-bom", hash, "1")
        dir.createDirectories()
        dir.resolve("${hash}-1.pom").writeText(pom)
        return "exploded-bom:${hash}:1"
    }

    override fun fabric(action: Action<ExplosionDesc>): String {
        val desc = ExplosionDescImpl(project)
        action(desc)

        val tempDir = createTempDirectory()

        desc.resolveJars {
            it.copyTo(tempDir.resolve(it.name).toFile())
        }

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
        val bomDeps = arrayListOf<Pair<String, String>>()

        for (mod in mods) {
            val version = sanitizeVersion(mod.version.friendlyString)

            bomDeps.add(mod.id to version)
            val out = createPom(mod.id, version)
            mod.copyToDir(out.parent, false).moveTo(out, overwrite = true)
        }

        tempDir.toFile().deleteRecursively()
        return createBom(desc.hash, bomDeps)
    }

}