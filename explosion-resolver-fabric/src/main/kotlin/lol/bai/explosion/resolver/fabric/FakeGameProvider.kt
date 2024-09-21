package lol.bai.explosion.resolver.fabric

import net.fabricmc.loader.impl.game.GameProvider
import net.fabricmc.loader.impl.game.patch.GameTransformer
import net.fabricmc.loader.impl.launch.FabricLauncher
import net.fabricmc.loader.impl.util.Arguments
import java.nio.file.Path

class FakeGameProvider(
    private val launchDirectory: Path
) : GameProvider {

    @Suppress("NOTHING_TO_INLINE")
    private inline fun unsupported(): Nothing {
        throw UnsupportedOperationException()
    }

    override fun getLaunchDirectory(): Path {
        return launchDirectory
    }

    override fun getBuiltinMods(): MutableCollection<GameProvider.BuiltinMod> {
        return mutableListOf()
    }

    override fun getGameId() = unsupported()
    override fun getGameName() = unsupported()
    override fun getRawGameVersion() = unsupported()
    override fun getNormalizedGameVersion() = unsupported()
    override fun getEntrypoint() = unsupported()
    override fun isObfuscated() = unsupported()
    override fun requiresUrlClassLoader() = unsupported()
    override fun isEnabled() = unsupported()
    override fun locateGame(launcher: FabricLauncher?, args: Array<out String>?) = unsupported()
    override fun initialize(launcher: FabricLauncher?) = unsupported()
    override fun getEntrypointTransformer(): GameTransformer = unsupported()
    override fun unlockClassPath(launcher: FabricLauncher?) = unsupported()
    override fun launch(loader: ClassLoader?) = unsupported()
    override fun getArguments(): Arguments = unsupported()
    override fun getLaunchArguments(sanitize: Boolean): Array<String> = unsupported()

}
