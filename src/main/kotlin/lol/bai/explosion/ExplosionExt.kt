package lol.bai.explosion

import groovy.lang.Closure
import groovy.lang.DelegatesTo
import org.gradle.api.Action

interface ExplosionExt {

    fun fabric(action: Action<ExplosionDesc>): String

    fun fabric(notation: String) = fabric {
        maven(notation)
    }

    fun fabric(@DelegatesTo(ExplosionDesc::class) closure: Closure<*>) = fabric {
        closure.delegate = this
        closure.call(this)
    }

}