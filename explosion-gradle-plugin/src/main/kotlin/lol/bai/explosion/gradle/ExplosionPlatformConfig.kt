package lol.bai.explosion.gradle

import groovy.lang.Closure
import groovy.lang.DelegatesTo
import org.gradle.api.Transformer
import org.gradle.api.file.RegularFileProperty
import java.nio.file.Path

interface ExplosionPlatformConfig {

    fun transformer(transformer: Transformer<Path, Path>)

    fun transformer(@DelegatesTo(Path::class) closure: Closure<Path>) = transformer r@{
        closure.delegate = it
        return@r closure.call(it)
    }

}