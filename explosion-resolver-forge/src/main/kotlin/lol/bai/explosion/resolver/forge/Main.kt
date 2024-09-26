package lol.bai.explosion.resolver.forge

import net.minecraftforge.fml.loading.UniqueModListBuilder
import net.minecraftforge.fml.loading.moddiscovery.JarInJarDependencyLocator
import net.minecraftforge.fml.loading.moddiscovery.ModFile
import net.minecraftforge.fml.loading.moddiscovery.createModsFolderLocator
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlin.io.path.Path
import kotlin.io.path.inputStream
import kotlin.io.path.name
import kotlin.io.path.writeText
import kotlin.jvm.optionals.getOrNull

@Suppress("UnstableApiUsage")
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

    for (modFile in uniqueModFiles) {
        val jar = modFile.secureJar.moduleDataProvider().descriptor()
        val mod = modFile.modInfos.firstOrNull()

        val id = mod?.modId ?: jar.name()
        val version = mod?.version ?: jar.rawVersion().getOrNull()
        if (version == null) continue

        val path = outputDir.resolve("${id}-${version}")
        Files.copy(modFile.filePath.inputStream(), path, StandardCopyOption.REPLACE_EXISTING)
        meta.append(path.name)
            .append("\t")
            .append(id)
            .append("\t")
            .append(version)
            .append("\n")
    }

    outputDir.resolve("__meta.txt").writeText(meta.toString())
}