package lol.bai.explosion.resolver.fabric

import net.fabricmc.api.EnvType
import net.fabricmc.loader.impl.FabricLoaderImpl
import net.fabricmc.loader.impl.discovery.ModResolver
import net.fabricmc.loader.impl.discovery.createModDiscoverer
import net.fabricmc.loader.impl.launch.FabricLauncherBase
import net.fabricmc.loader.impl.launch.knot.Knot
import kotlin.io.path.Path
import kotlin.io.path.moveTo
import kotlin.io.path.name
import kotlin.io.path.writeText

fun main(args: Array<String>) {
    val (inputDirStr, outputDirStr) = args
    val inputDir = Path(inputDirStr)
    val outputDir = Path(outputDirStr)

    if (FabricLauncherBase.getLauncher() == null) Knot(EnvType.CLIENT)

    val loader = FabricLoaderImpl.INSTANCE.apply {
        gameProvider = FakeGameProvider(inputDir)
    }

    val discoverer = createModDiscoverer(inputDir)

    val candidates = discoverer.discoverMods(loader, mutableMapOf())
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

    val meta = StringBuilder()
    val mods = ModResolver.resolve(candidates, EnvType.CLIENT, mutableMapOf())

    for (mod in mods) {
        val path = outputDir.resolve("${mod.id}-${mod.version.friendlyString}")
        mod.copyToDir(outputDir, false).moveTo(path, overwrite = true)
        meta.append(path.name)
            .append("\t")
            .append(mod.id)
            .append("\t")
            .append(mod.version.friendlyString)
            .append("\n")
    }

    outputDir.resolve("__meta.txt").writeText(meta.toString())
}