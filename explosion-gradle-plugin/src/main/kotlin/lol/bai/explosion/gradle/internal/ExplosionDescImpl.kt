package lol.bai.explosion.gradle.internal

import lol.bai.explosion.gradle.ExplosionDesc
import org.gradle.api.Project
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.FileCollectionDependency
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.add
import org.gradle.kotlin.dsl.withType
import java.io.File

class ExplosionDescImpl(
    private val project: Project
) : ExplosionDesc {

    private val dependencyModifiers = mutableListOf<(DependencyHandler, String) -> Unit>()

    override fun maven(notation: String) {
        dependencyModifiers.add { dependencies, config ->
            dependencies.add(config, notation) { isTransitive = false }
        }
    }

    override fun <T : ExternalModuleDependency> maven(provider: Provider<T>) {
        dependencyModifiers.add { dependencies, config ->
            dependencies.addProvider<T, T>(config, provider) { isTransitive = false }
        }
    }

    override fun local(file: File) {
        dependencyModifiers.add { dependencies, config ->
            if (file.isFile) dependencies.add(config, project.files(file))
            else if (file.isDirectory) dependencies.add(config, project.fileTree(file) {
                include("*.jar")
            })
        }
    }

    fun resolveJars(fn: (File) -> Unit) {
        val configuration = project.configurations.create("__explosion_" + Any().hashCode())

        dependencyModifiers.forEach { modifier ->
            modifier(project.dependencies, configuration.name)
        }

        configuration.resolvedConfiguration.resolvedArtifacts.forEach {
            if (it.type == "jar") fn(it.file)
        }

        configuration.dependencies.withType<FileCollectionDependency>().forEach { dep ->
            dep.files.forEach {
                if (it.extension == "jar") fn(it)
            }
        }

        project.configurations.remove(configuration)
    }

}