# explosion!!!

A Gradle plugin to resolve Jar-in-Jar dependencies.

## Basic Usage
```groovy
// settings.gradle
pluginManagement {
    repositories {
        maven { url "https://maven2.bai.lol" }
    }
}
```

```groovy
// build.gradle
plugins {
    id "lol.bai.explosion" version "0.1.0"
}

dependencies {
    modImplementation explosion.fabric("curse.maven:fabric-api-306612:4764776")
}
```

## Advanced Usage

### Local File Dependency
Explosion also provides way to explode local file and directory.
```groovy
explosion.fabric {
    maven "curse.maven:fabric-api-306612:4764776"
    local project.file("path/to/mod.jar")
    loacl project.file("path/to/mods/directory")
}
```

### Excluding Certain Mod
All resolved mods will have `exploded` as its group and the mod id as its module.
You can therefore exclude it with such.
```groovy
def exploded = explosion.fabric { /*...*/ }

modImplementation(exploded) {
    exclude group: "exploded", module: "modid" 
}
```

### Debugging Modpacks
Explosion can also be used to debug modpacks by depending on the modpack's mods folder. 
1. Install the modpack using your favorite launcher.
2. Open its instance folder, and rename the `mods` folder with something else, e.g. `mods-blabla`.
3. Set up a dev environment, you can use the [Template Generator](https://fabricmc.net/develop/template/).
   Disable the split sources for higher chance to work.
4. Depend on the folder.
   ```groovy
   modImplementation explosion.fabric {
       local project.file("path/to/mods-blabla")
   }
   ```
5. Configure the run directory to the instance folder.
6. Pray for it to be able to run.
7. Fix mappings error, if any.
8. Pray again.
