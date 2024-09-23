package lol.bai.explosion.gradle.internal

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.work.DisableCachingByDefault

@DisableCachingByDefault
abstract class ResolverTask : JavaExec() {

    @get:Input
    abstract val configuration: Property<String>

    @get:InputDirectory
    abstract val inputDir: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    override fun exec() {
        classpath = project.configurations.getByName(configuration.get())

        args(
            inputDir.asFile.get().absolutePath,
            outputDir.asFile.get().absolutePath,
        )

        super.exec()
    }

}