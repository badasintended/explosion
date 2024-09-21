plugins {
    `maven-publish`

    kotlin("jvm") version "1.9.22"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

allprojects {
    apply(plugin = "maven-publish")
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "com.github.johnrengelman.shadow")

    version = providers.environmentVariable("VERSION").getOrElse("9999-local")
    group = "lol.bai.explosion"

    repositories {
        mavenCentral()
    }

    kotlin {
        jvmToolchain(17)
    }

    configurations {
        val shade by creating

        compileOnly {
            extendsFrom(shade)
        }
    }

    tasks.shadowJar {
        configurations = listOf(project.configurations["shade"])
        archiveClassifier.set("")

        minimize()
        mergeServiceFiles()
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
}
