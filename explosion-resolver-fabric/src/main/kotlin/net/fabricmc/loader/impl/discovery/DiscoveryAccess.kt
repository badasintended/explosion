package net.fabricmc.loader.impl.discovery

import net.fabricmc.loader.impl.metadata.DependencyOverrides
import net.fabricmc.loader.impl.metadata.VersionOverrides
import java.nio.file.Path

fun createModDiscoverer(dir: Path) = ModDiscoverer(VersionOverrides(), DependencyOverrides(dir)).apply {
    addCandidateFinder(DirectoryModCandidateFinder(dir, false))
}
