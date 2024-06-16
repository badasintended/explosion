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
}

configurations {
    create("shade")
}

dependencies {
    implementation("com.google.guava:guava:32.1.2-jre")

    compileOnly("net.fabricmc:fabric-loader:0.14.22")
    "shade"("net.fabricmc:fabric-loader:0.14.22")
    "shade"("net.fabricmc:access-widener:2.1.0")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(8)
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
    relocate("net.fabricmc", "lol.bai.explosion.internal.lib.fabricloader")
    minimize()
    exclude("ui/**")
    exclude("assets/**")
    exclude("fabric*.json")
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
                url = uri("https://maven.pkg.github.com/badasintended/wthit")
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
