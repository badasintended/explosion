package lol.bai.explosion.gradle

import groovy.lang.Closure
import groovy.lang.DelegatesTo
import org.gradle.api.Action
import org.gradle.api.Transformer
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.provider.Provider
import java.io.File
import java.nio.file.Path

interface ExplosionExt {

    fun withTransformer(id: String, transformer: Transformer<Path, Path>): ExplosionExt

    fun withTransformer(id: String, @DelegatesTo(File::class) closure: Closure<Path>) = withTransformer(id) r@{
        closure.delegate = this
        return@r closure.call(this)
    }

    // ---

    fun fabric(action: Action<ExplosionDesc>): Provider<String>

    fun fabric(notation: String) = fabric {
        maven(notation)
    }

    fun <T : ExternalModuleDependency> fabric(notation: Provider<T>) = fabric {
        maven(notation)
    }

    fun fabric(@DelegatesTo(ExplosionDesc::class) closure: Closure<*>) = fabric {
        closure.delegate = this
        closure.call(this)
    }

    // ---

    fun forge(action: Action<ExplosionDesc>): Provider<String>

    fun forge(notation: String) = forge {
        maven(notation)
    }

    fun <T : ExternalModuleDependency> forge(notation: Provider<T>) = forge {
        maven(notation)
    }

    fun forge(@DelegatesTo(ExplosionDesc::class) closure: Closure<*>) = forge {
        closure.delegate = this
        closure.call(this)
    }

}