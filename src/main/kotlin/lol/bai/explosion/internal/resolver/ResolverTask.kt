package lol.bai.explosion.internal.resolver

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.work.DisableCachingByDefault

@DisableCachingByDefault
abstract class ResolverTask : JavaExec() {

    companion object {
        const val CONFIGURATION = "__explosion_resolver"
    }

    @get:Input
    abstract val loader: Property<String>

    @get:InputDirectory
    abstract val inputDir: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    init {
        classpath = project.configurations.getByName(CONFIGURATION)
        mainClass.set("lol.bai.explosion.internal.resolver.MainKt")
    }

    @TaskAction
    override fun exec() {
        args(
            loader.get(),
            inputDir.asFile.get().absolutePath,
            outputDir.asFile.get().absolutePath,
        )

        super.exec()
    }

}