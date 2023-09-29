package lol.bai.explosion.internal

import com.google.common.hash.Hashing
import lol.bai.explosion.ExplosionDesc
import org.gradle.api.Project
import org.gradle.api.artifacts.FileCollectionDependency
import org.gradle.kotlin.dsl.add
import org.gradle.kotlin.dsl.withType
import java.io.File

class ExplosionDescImpl(
    private val project: Project
) : ExplosionDesc {

    private val configuration = project.configurations.create("__explosion")
    private val hashBuilder = StringBuilder()

    override fun maven(notation: String) {
        hashBuilder.append(notation)
        hashBuilder.append(';')

        project.dependencies.add(configuration.name, notation) { isTransitive = false }
    }

    override fun local(file: File) {
        hashBuilder.append(file.absolutePath)
        hashBuilder.append(';')

        if (file.isFile) project.dependencies.add(configuration.name, project.files(file))
        else if (file.isDirectory) project.dependencies.add(configuration.name, project.fileTree(file) {
            include("*.jar")
        })
    }

    fun resolveJars(fn: (File) -> Unit) {
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

    val hash get() = Hashing.murmur3_128().hashString(hashBuilder.toString(), Charsets.UTF_8).toString()

}