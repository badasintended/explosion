package lol.bai.explosion.resolver.forge

import net.minecraftforge.fml.loading.UniqueModListBuilder
import net.minecraftforge.fml.loading.moddiscovery.JarInJarDependencyLocator
import net.minecraftforge.fml.loading.moddiscovery.ModFile
import net.minecraftforge.fml.loading.moddiscovery.createModsFolderLocator
import net.minecraftforge.forgespi.locating.IModFile
import kotlin.io.path.Path
import kotlin.io.path.copyTo
import kotlin.io.path.name
import kotlin.io.path.writeText

fun main(args: Array<String>) {
    val (inputDirStr, outputDirStr) = args
    val inputDir = Path(inputDirStr)
    val outputDir = Path(outputDirStr)

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