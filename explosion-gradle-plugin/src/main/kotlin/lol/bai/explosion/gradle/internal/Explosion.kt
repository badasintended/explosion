package lol.bai.explosion.gradle.internal

import lol.bai.explosion.gradle.ExplosionExt
import lol.bai.explosion.gradle.ExplosionPlatformConfig
import org.gradle.api.Project
import java.nio.file.Path


open class Explosion(project: Project, outputDir: Path) : ExplosionExt {

    override val fabric = Platform(project, "fabric", outputDir, null)
    override val forge = Platform(project, "forge", outputDir, null)

}