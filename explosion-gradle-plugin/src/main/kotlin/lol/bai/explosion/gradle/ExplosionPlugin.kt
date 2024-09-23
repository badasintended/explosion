package lol.bai.explosion.gradle

import lol.bai.explosion.gradle.internal.Explosion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.maven
import java.io.File
import kotlin.io.path.createDirectories

class ExplosionPlugin : Plugin<Project> {

    override fun apply(project: Project): Unit = with(project) {
        val outputDir = gradle.gradleUserHomeDir
            .resolve("caches" + File.separator + "lol.bai.explosion")
            .toPath()
            .createDirectories()

        extensions.create(
            ExplosionExt::class, "explosion", Explosion::class,
            project, outputDir
        )

        repositories.maven(outputDir.toFile()) {
            name = "ExplodedPluginCache"
        }
    }

}
