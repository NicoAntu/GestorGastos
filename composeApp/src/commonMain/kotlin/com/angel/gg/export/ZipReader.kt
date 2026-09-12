package com.angel.gg.export

object ZipReader {

    data class NoEsZip(val motivo: String) : Exception(motivo)

    fun leer(bytes: ByteArray): List<Pair<String, ByteArray>> {
        val entradas = ArrayList<Pair<String, ByteArray>>()
        var pos = 0
        var leidas = 0
        while (pos + 30 <= bytes.size) {
            if (u32(bytes, pos) != 0x04034b50L) break
            val flags = u16(bytes, pos + 6)
            val metodo = u16(bytes, pos + 8)
            val fnameLen = u16(bytes, pos + 26)
            val extraLen = u16(bytes, pos + 28)
            val compSize = u32(bytes, pos + 18).toInt()
            if (flags and 0x08 != 0 && compSize <= 0) {
                throw NoEsZip("cabecera local sin tamanos (data descriptor)")
            }
            val dataStart = pos + 30 + fnameLen + extraLen
            val nombre = bytes.copyOfRange(pos + 30, pos + 30 + fnameLen).toString(Charsets.UTF_8)
            val crudo = bytes.copyOfRange(dataStart, dataStart + compSize)
            val data = when (metodo) {
                0 -> crudo
                8 -> inflarDeflate(crudo)
                else -> throw NoEsZip("método de compresión $metodo no soportado")
            }
            entradas.add(nombre to data)
            pos = dataStart + compSize
            leidas++
        }
        if (leidas == 0) throw NoEsZip("no se encontraron entradas")
        return entradas
    }

    private fun u16(b: ByteArray, o: Int): Int =
        (b[o].toInt() and 0xFF) or ((b[o + 1].toInt() and 0xFF) shl 8)

    private fun u32(b: ByteArray, o: Int): Long =
        (b[o].toLong() and 0xFFL) or
        ((b[o + 1].toLong() and 0xFFL) shl 8) or
        ((b[o + 2].toLong() and 0xFFL) shl 16) or
        ((b[o + 3].toLong() and 0xFFL) shl 24)
}