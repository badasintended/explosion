package lol.bai.explosion

import groovy.lang.Closure
import groovy.lang.DelegatesTo
import org.gradle.api.Action
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.provider.Provider

interface ExplosionExt {

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

}