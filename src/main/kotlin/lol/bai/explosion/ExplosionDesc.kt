package lol.bai.explosion

import java.io.File

interface ExplosionDesc {

    fun maven(notation: String)

    fun local(file: File)

}