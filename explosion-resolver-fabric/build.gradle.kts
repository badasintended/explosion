repositories {
    maven("https://maven.fabricmc.net/")
}

dependencies {
    shade("net.fabricmc:fabric-loader:0.15.10")

    implementation("net.fabricmc:access-widener:2.1.0")
}

tasks.shadowJar {
    exclude("ui/**")
    exclude("assets/**")
    exclude("fabric*.json")
    exclude("LICENSE_fabric-loader")
    exclude("META-INF/jars/**")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}