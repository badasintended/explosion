package net.minecraftforge.fml.loading.moddiscovery

import java.nio.file.Path

fun createModsFolderLocator(modsFolder: Path, name: String) = ModsFolderLocator(modsFolder, name)