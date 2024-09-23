package lol.bai.explosion.resolver.neoforge

import net.neoforged.fml.loading.moddiscovery.locators.JarInJarDependencyLocator
import net.neoforged.fml.loading.moddiscovery.locators.ModsFolderLocator
import net.neoforged.fml.loading.moddiscovery.readers.JarModsDotTomlModFileReader
import net.neoforged.fml.loading.moddiscovery.readers.NestedLibraryModReader
import net.neoforged.neoforgespi.ILaunchContext
import net.neoforged.neoforgespi.locating.IDependencyLocator
import net.neoforged.neoforgespi.locating.IModFileCandidateLocator
import net.neoforged.neoforgespi.locating.IModFileReader
import java.nio.file.Path
import java.util.*
import java.util.stream.Stream

class FakeLaunchContext(
    private val modsFolder: Path
) : ILaunchContext {
    @Suppress("NOTHING_TO_INLINE")
    private inline fun unsupported(): Nothing {
        throw UnsupportedOperationException()
    }

    override fun environment() = unsupported()
    override fun modLists() = unsupported()
    override fun mods() = unsupported()
    override fun mavenRoots() = unsupported()

    override fun isLocated(p0: Path) = false
    override fun addLocated(p0: Path) = true

    override fun <T : Any?> loadServices(service: Class<T>): Stream<ServiceLoader.Provider<T>> {
        return when (service) {
            IModFileCandidateLocator::class.java -> Stream.of(
                Fake(ModsFolderLocator(modsFolder, "explosion!!!"))
            )

            IModFileReader::class.java -> Stream.of(
                Fake(JarModsDotTomlModFileReader()),
                Fake(NestedLibraryModReader())
            )

            IDependencyLocator::class.java -> Stream.of(
                Fake(JarInJarDependencyLocator())
            )

            else -> unsupported()
        }
    }
}

@Suppress("UNCHECKED_CAST")
private class Fake<T>(val instance: Any) : ServiceLoader.Provider<T> {
    override fun get() = instance as T
    override fun type() = instance::class.java as Class<T>
}