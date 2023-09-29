plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    `maven-publish`

    kotlin("jvm") version "1.9.0"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "lol.bai"
version = "0.1.0"

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

