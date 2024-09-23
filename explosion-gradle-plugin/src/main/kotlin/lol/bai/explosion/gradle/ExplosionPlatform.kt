package lol.bai.explosion.gradle

import groovy.lang.Closure
import groovy.lang.DelegatesTo
import org.gradle.api.Action
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.provider.Provider

interface ExplosionPlatform {

    fun with(id: String, action: Action<ExplosionPlatformConfig>): ExplosionPlatform

    fun with(id: String, @DelegatesTo(ExplosionPlatformConfig::class) closure: Closure<*>) = with(id) r@{
        closure.delegate = this
        closure.call(this)
    }

    // kotlin callables

    operator fun invoke(action: Action<ExplosionPlatformDesc>): Provider<String>

    operator fun invoke(notation: String) = invoke {
        maven(notation)
    }

    operator fun <T : ExternalModuleDependency> invoke(notation: Provider<T>) = invoke {
        maven(notation)
    }

    // groovy callables

    fun call(notation: String) = invoke(notation)

    fun <T : ExternalModuleDependency> call(notation: Provider<T>) = invoke(notation)

    fun call(@DelegatesTo(ExplosionPlatformDesc::class) closure: Closure<*>) = invoke {
        closure.delegate = this
        closure.call(this)
    }

}