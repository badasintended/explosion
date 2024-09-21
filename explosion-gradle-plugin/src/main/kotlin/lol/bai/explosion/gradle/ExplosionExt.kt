package lol.bai.explosion.gradle

import groovy.lang.Closure
import groovy.lang.DelegatesTo
import org.gradle.api.Action
import org.gradle.api.Transformer
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.provider.Provider
import java.nio.file.Path
import kotlin.io.path.name

interface ExplosionExt {

    fun withTransformer(id: String, transformer: Transformer<Path, Path>): ExplosionExt

    fun withTransformer(id: String, @DelegatesTo(Path::class) closure: Closure<Path>) = withTransformer(id) r@{
        closure.delegate = it
        return@r closure.call(it)
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