kotlin {
    jvmToolchain(21)
}

repositories {
    maven("https://maven.neoforged.net/releases/")
    maven("https://libraries.minecraft.net/")
}

dependencies {
    shade("net.neoforged.fancymodloader:loader:4.0.29") { isTransitive = false }

    implementation("com.google.guava:guava:32.1.2-jre")
    implementation("org.apache.maven:maven-artifact:3.8.1")
    implementation("com.electronwill.night-config:core:3.7.3")
    implementation("com.electronwill.night-config:toml:3.7.3")
    implementation("org.apache.logging.log4j:log4j-api:2.22.1")
    implementation("org.apache.logging.log4j:log4j-core:2.22.1")
    implementation("org.slf4j:slf4j-api:2.0.16")
    implementation("com.google.code.gson:gson:2.11.0")

    implementation("com.mojang:logging:1.2.7") { isTransitive = false }
    implementation("cpw.mods:modlauncher:11.0.3") { isTransitive = false }
    implementation("cpw.mods:securejarhandler:3.0.7") { isTransitive = false }
    implementation("net.neoforged:JarJarFileSystems:0.4.1") { isTransitive = false }
    implementation("net.neoforged:JarJarMetadata:0.4.1") { isTransitive = false }
    implementation("net.neoforged:JarJarSelector:0.4.1") { isTransitive = false }
    implementation("net.neoforged:mergetool:2.0.0:api")
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