repositories {
    maven("https://maven.minecraftforge.net/")
    maven("https://libraries.minecraft.net/")
}

dependencies {
    shade("net.minecraftforge:fmlloader:1.20.4-49.0.49") { isTransitive = false }

    implementation("com.google.guava:guava:32.1.2-jre")
    implementation("org.apache.maven:maven-artifact:3.8.1")
    implementation("com.electronwill.night-config:core:3.7.3")
    implementation("com.electronwill.night-config:toml:3.7.3")
    implementation("org.apache.logging.log4j:log4j-api:2.22.1")
    implementation("org.apache.logging.log4j:log4j-core:2.22.1")
    implementation("org.slf4j:slf4j-api:2.0.16")
    implementation("com.google.code.gson:gson:2.11.0")

    implementation("com.mojang:logging:1.2.7") { isTransitive = false }
    implementation("net.minecraftforge:modlauncher:10.2.1") { isTransitive = false }
    implementation("net.minecraftforge:securemodules:2.2.12") { isTransitive = false }
    implementation("net.minecraftforge:forgespi:7.1.5") { isTransitive = false }
    implementation("net.minecraftforge:JarJarFileSystems:0.3.26") { isTransitive = false }
    implementation("net.minecraftforge:JarJarMetadata:0.3.26") { isTransitive = false }
    implementation("net.minecraftforge:JarJarSelector:0.3.26") { isTransitive = false }
    implementation("net.minecraftforge:unsafe:0.9.2") { isTransitive = false }
    implementation("net.minecraftforge:mergetool-api:1.0")
}

tasks.shadowJar {
    exclude("log4j2*")
    exclude("META-INF/org/apache/logging/**")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}