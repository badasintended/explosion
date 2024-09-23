package lol.bai.explosion.gradle.internal

import lol.bai.explosion.gradle.ExplosionPlatformConfig
import org.gradle.api.Transformer
import java.nio.file.Path

class PlatformConfig(
    val id: String
) : ExplosionPlatformConfig {

    var transformer: Transformer<Path, Path>? = null

    override fun transformer(transformer: Transformer<Path, Path>) {
        this.transformer = transformer
    }

}