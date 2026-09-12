package com.angel.gg.export

expect class ImportarLectura {
    suspend fun leer(): ByteArray?
}