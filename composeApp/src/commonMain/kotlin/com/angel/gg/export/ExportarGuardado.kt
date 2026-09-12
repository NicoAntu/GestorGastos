package com.angel.gg.export

expect class ExportarGuardado {
    suspend fun guardar(nombreSugerido: String, bytes: ByteArray): Boolean
}