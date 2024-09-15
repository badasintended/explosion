plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    `maven-publish`

    kotlin("jvm") version "1.9.0"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "lol.bai"
version = providers.environmentVariable("VERSION").getOrElse("9999-local")

repositories {
    mavenCentral()
    maven("https://maven.fabricmc.net/")
    maven("https://maven.minecraftforge.net/")
    maven("https://libraries.minecraft.net/")
}

configurations {
    create("shade")
}

dependencies {
    val shade by configurations
    fun compileOnlyShade(artifact: String, action: ExternalModuleDependency.() -> Unit = {}) {
        compileOnly(artifact, action)
        shade(artifact, action)
    }

    implementation("com.google.guava:guava:32.1.2-jre")
    implementation("org.apache.maven:maven-artifact:3.8.1")
    implementation("com.electronwill.night-config:core:3.7.3")
    implementation("com.electronwill.night-config:toml:3.7.3")
    implementation("org.apache.logging.log4j:log4j-api:2.22.1")
    implementation("org.apache.logging.log4j:log4j-core:2.22.1")

    compileOnlyShade("net.fabricmc:fabric-loader:0.15.10")
    shade("net.fabricmc:access-widener:2.1.0")

    compileOnlyShade("com.mojang:logging:1.2.7") { isTransitive = false }
    compileOnlyShade("net.minecraftforge:fmlloader:1.20.4-49.0.49") { isTransitive = false }
    compileOnlyShade("net.minecraftforge:modlauncher:10.2.1") { isTransitive = false }
    compileOnlyShade("net.minecraftforge:securemodules:2.2.12") { isTransitive = false }
    compileOnlyShade("net.minecraftforge:forgespi:7.1.5") { isTransitive = false }
    compileOnlyShade("net.minecraftforge:JarJarFileSystems:0.3.26") { isTransitive = false }
    compileOnlyShade("net.minecraftforge:JarJarMetadata:0.3.26") { isTransitive = false }
    compileOnlyShade("net.minecraftforge:JarJarSelector:0.3.26") { isTransitive = false }
    compileOnlyShade("net.minecraftforge:unsafe:0.9.2") { isTransitive = false }
    compileOnlyShade("net.minecraftforge:mergetool-api:1.0")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}

gradlePlugin {
    plugins {
        create("explosion") {
            id = "lol.bai.explosion"
            implementationClass = "lol.bai.explosion.ExplosionPlugin"
        }
    }
}

tasks.shadowJar {
    configurations = listOf(project.configurations["shade"])
    archiveClassifier.set("")
    mergeServiceFiles()

    relocate("net.fabricmc", "lol.bai.explosion.internal.lib.net.fabricmc")
    relocate("net.minecraftforge", "lol.bai.explosion.internal.lib.net.minecraftforge")
    relocate("cpw", "lol.bai.explosion.internal.lib.cpw")
    relocate("com.mojang", "lol.bai.explosion.internal.lib.com.mojang")

    exclude("ui/**")
    exclude("assets/**")
    exclude("fabric*.json")
    exclude("log4j2*")
    exclude("LICENSE_fabric-loader")
    exclude("META-INF/jars/**")
    exclude("META-INF/org/apache/logging/**")
}

tasks.build {
    dependsOn("shadowJar")
}

publishing {
    repositories {
        maven {
            name = "LocalMaven"
            url = projectDir.resolve(".localMaven").toURI()
        }

        if (providers.environmentVariable("GITHUB_TOKEN").isPresent) {
            maven {
                url = uri("https://maven.pkg.github.com/badasintended/explosion")
                name = "GitHub"
                credentials {
                    username = providers.environmentVariable("GITHUB_ACTOR").get()
                    password = providers.environmentVariable("GITHUB_TOKEN").get()
                }
            }
        }

        if (providers.environmentVariable("MAVEN_PASSWORD").isPresent) {
            maven {
                url = uri("https://maven4.bai.lol")
                name = "Badasintended"
                credentials {
                    username = providers.environmentVariable("MAVEN_USERNAME").orNull
                    password = providers.environmentVariable("MAVEN_PASSWORD").orNull
                }
            }
        }
    }
}
