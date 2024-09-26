# explosion!!!

There is a time when you need to depend on a mod that uses Jar-in-Jar
and you need to access classes from those JiJ, but it is a private JiJ,
and it doesn't get published to any Maven repository, so you scrap your
entire mod idea into the abyss, never to be seen again.

Right? Or maybe it's only me?

Well, I made this Gradle plugin to solve exactly that. This plugin will
resolve Jar-in-Jars, making it available to be depended upon.

## Basic Usage
```groovy
// settings.gradle
pluginManagement {
   repositories {
      maven {
         url "https://maven2.bai.lol"
         content {
            includeGroup "lol.bai.explosion"
         }
      }
   }
}
```

```groovy
// build.gradle
plugins {
   id "lol.bai.explosion" version "0.3.1"
}

repositories {
   maven {
      url "https://maven2.bai.lol"
      content {
         includeGroup "lol.bai.explosion"
      }
   }
}

dependencies {
   modImplementation explosion.fabric("curse.maven:fabric-api-306612:5710210")
   implementation explosion.forge("curse.maven:create-328085:5689514")
   implementation explosion.neoforge("curse.maven:ender-io-64578:5720393")
}
```
> [!WARNING]
> `fg.deobf` won't work with exploded dependency, you need to manually deobfuscate
> the jar.
> ```groovy
> // replace the mapping as needed
> def mapping = "official_1.20.1"
> def deobfuscator = new net.minecraftforge.gradle.userdev.util.Deobfuscator(project, file("build/explosion_deobf"))
> def explode = explosion.forge.with(mapping) {
>     transformer {
>         return deobfuscator.deobfBinary(it.toFile(), mapping, it.fileName.toString()).toPath()
>     }
> }
> 
> // then use the explode variable
> implementation explode("curse.maven:create-328085:5689514")
> ```

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

## How does this work?
Explosion simply uses the existing loader infrastructure on each
platform to load and resolve the mod jars. It literally runs the
loader as if it's the runtime and hack it to get the loaded jars.
Well, as long as it works, amirite?
