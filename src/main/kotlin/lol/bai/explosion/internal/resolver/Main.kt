package lol.bai.explosion.internal.resolver

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
import java.nio.file.Path
import kotlin.io.path.*

fun main(args: Array<String>) {
    val (loader, inputDirStr, outputDirStr) = args

    val inputDir = Path(inputDirStr)
    val outputDir = Path(outputDirStr)

    when (loader) {
        "fabric" -> fabric(inputDir, outputDir)
        "forge" -> forge(inputDir, outputDir)
        else -> throw IllegalArgumentException("Unsupported loader $loader")
    }
}

private fun fabric(inputDir: Path, outputDir: Path) {
    if (FabricLauncherBase.getLauncher() == null) Knot(EnvType.CLIENT)

    val loader = FabricLoaderImpl.INSTANCE.apply {
        gameProvider = FakeGameProvider(inputDir)
    }

    val candidates = createModDiscoverer(inputDir).discoverMods(loader, mutableMapOf())
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

@Suppress("UnstableApiUsage")
private fun forge(inputDir: Path, outputDir: Path) {
    val modLocator = createModsFolderLocator(inputDir, "explosion!!!")
    val jarJar = JarInJarDependencyLocator()

    val primeModFiles = modLocator.scanMods().map { it.file }
    val jarJarModFiles = jarJar.scanMods(primeModFiles)
    val combinedModFiles = (primeModFiles + jarJarModFiles).filterIsInstance<ModFile>()

    val meta = StringBuilder()
    val uniqueModFiles = UniqueModListBuilder(combinedModFiles)
        .buildUniqueList().modFiles
        .filter { it.type == IModFile.Type.MOD }

    for (modFile in uniqueModFiles) {
        val mod = modFile.modInfos[0]
        val path = outputDir.resolve("${mod.modId}-${mod.version}")
        modFile.filePath.copyTo(path, overwrite = true)
        meta.append(path.name)
            .append("\t")
            .append(mod.modId)
            .append("\t")
            .append(mod.version)
            .append("\n")
    }

    outputDir.resolve("__meta.txt").writeText(meta.toString())
}