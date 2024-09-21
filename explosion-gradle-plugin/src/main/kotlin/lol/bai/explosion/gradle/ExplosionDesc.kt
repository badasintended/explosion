package lol.bai.explosion.gradle

import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.provider.Provider
import java.io.File

interface ExplosionDesc {

    fun maven(notation: String)

    fun <T : ExternalModuleDependency> maven(provider: Provider<T>)

    fun local(file: File)

}