pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

rootProject.name = "explosion"

include("explosion-gradle-plugin")
include("explosion-resolver-fabric")
include("explosion-resolver-forge")
include("explosion-resolver-neoforge")
