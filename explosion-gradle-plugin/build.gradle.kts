plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
}

dependencies {
    implementation("com.google.guava:guava:32.1.2-jre")
}

gradlePlugin {
    plugins {
        create("explosion") {
            id = "lol.bai.explosion"
            implementationClass = "lol.bai.explosion.gradle.ExplosionPlugin"
        }
    }
}

tasks.processResources {
    val meta = "${project.group}:${project.name}:${project.version}"
    inputs.property("meta", meta)

    filesMatching("__meta.txt") {
        expand("meta" to meta)
    }
}
