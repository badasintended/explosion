package lol.bai.explosion

import lol.bai.explosion.internal.ExplosionExtImpl
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.maven
import java.io.File

class ExplosionPlugin : Plugin<Project> {

    override fun apply(project: Project): Unit = with(project) {
        val outputDir = project.rootDir
            .resolve(".gradle" + File.separator + "explodedMaven")
            .toPath()

        extensions.create(
            ExplosionExt::class, "explosion", ExplosionExtImpl::class,
            project, outputDir
        )

        repositories.maven(outputDir.toFile()) {
            name = "ExplodedPluginCache"
        }
    }

}
