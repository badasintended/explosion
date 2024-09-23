package lol.bai.explosion.resolver.neoforge

import net.neoforged.fml.loading.ImmediateWindowHandler
import net.neoforged.fml.loading.moddiscovery.ModDiscoverer
import net.neoforged.fml.loading.moddiscovery.ModFile
import net.neoforged.fml.loading.moddiscovery.ModValidator
import net.neoforged.fml.loading.progress.ProgressMeter
import net.neoforged.fml.loading.progress.StartupNotificationManager
import java.lang.invoke.MethodHandles
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlin.io.path.Path
import kotlin.io.path.inputStream
import kotlin.io.path.name
import kotlin.io.path.writeText

@Suppress("UnstableApiUsage")
fun main(args: Array<String>) {
    val (inputDirStr, outputDirStr) = args
    val inputDir = Path(inputDirStr)
    val outputDir = Path(outputDirStr)

    @Suppress("LocalVariableName")
    run {
        val ImmediateWindowHandler = ImmediateWindowHandler::class.java
        val ProgressMeter = ProgressMeter::class.java

        MethodHandles
            .privateLookupIn(ImmediateWindowHandler, MethodHandles.lookup())
            .findStaticVarHandle(ImmediateWindowHandler, "earlyProgress", ProgressMeter)
            .set(StartupNotificationManager.addProgressBar("explosion!!!", 0))
    }

    val discoverer = ModDiscoverer(FakeLaunchContext(inputDir))
    val validator = discoverer.discoverMods()

    @Suppress("LocalVariableName", "UNCHECKED_CAST")
    val candidateMods = validator.run {
        val ModValidator = ModValidator::class.java
        val List = List::class.java

        MethodHandles
            .privateLookupIn(ModValidator, MethodHandles.lookup())
            .findVarHandle(ModValidator, "candidateMods", List)
            .get(this) as List<ModFile>
    }

    val meta = StringBuilder()
    for (modFile in candidateMods) {
        val mod = modFile.modInfos.firstOrNull() ?: continue
        val path = outputDir.resolve("${mod.modId}-${mod.version}")
        Files.copy(modFile.filePath.inputStream(), path, StandardCopyOption.REPLACE_EXISTING)
        meta.append(path.name)
            .append("\t")
            .append(mod.modId)
            .append("\t")
            .append(mod.version)
            .append("\n")
    }

    outputDir.resolve("__meta.txt").writeText(meta.toString())
}