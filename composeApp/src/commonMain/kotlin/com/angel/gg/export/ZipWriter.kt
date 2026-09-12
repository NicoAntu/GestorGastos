package com.angel.gg.export

object ZipWriter {

    private const val SIG_LOCAL   = 0x04034b50L
    private const val SIG_CENTRAL = 0x02014b50L
    private const val SIG_EOCD    = 0x06054b50L
    private const val FLAG_UTF8   = 0x0800

    fun zip(entries: List<Pair<String, ByteArray>>): ByteArray {
        val piezas = ArrayList<ByteArray>()
        fun push(b: ByteArray) { piezas.add(b) }

        val central = ArrayList<ByteArray>()
        var offset = 0L

        entries.forEach { (nombre, data) ->
            val crc = crc32(data)
            val nombreBytes = nombre.toByteArray(Charsets.UTF_8)

            val local = BuilderPiezas()
            local.u32(SIG_LOCAL)
            local.u16(20)            // version needed
            local.u16(FLAG_UTF8)
            local.u16(0)             // método STORED
            local.u16(0)
            local.u16(0x21)          // fecha 1980-01-01
            local.u32(crc)
            local.u32(data.size.toLong())
            local.u32(data.size.toLong())
            local.u16(nombreBytes.size)
            local.u16(0)             // extra
            local.bytes(nombreBytes)
            push(local.build())
            push(data)

            val c = BuilderPiezas()
            c.u32(SIG_CENTRAL)
            c.u16(20)
            c.u16(20)
            c.u16(FLAG_UTF8)
            c.u16(0)
            c.u16(0)
            c.u16(0x21)
            c.u32(crc)
            c.u32(data.size.toLong())
            c.u32(data.size.toLong())
            c.u16(nombreBytes.size)
            c.u16(0)
            c.u16(0)
            c.u16(0)
            c.u16(0)
            c.u32(0)
            c.u32(offset)
            c.bytes(nombreBytes)
            central.add(c.build())

            offset += local.size() + data.size
        }

        val centralSize = central.sumOf { it.size.toLong() }
        val centralPieza = BuilderPiezas()
        central.forEach { centralPieza.bytes(it) }
        push(centralPieza.build())

        val eocd = BuilderPiezas()
        eocd.u32(SIG_EOCD)
        eocd.u16(0)
        eocd.u16(0)
        eocd.u16(entries.size)
        eocd.u16(entries.size)
        eocd.u32(centralSize)
        eocd.u32(offset)
        eocd.u16(0)
        push(eocd.build())

        val total = piezas.sumOf { it.size.toLong() }.toInt()
        val out = ByteArray(total)
        var pos = 0
        piezas.forEach { it.copyInto(out, pos); pos += it.size }
        return out
    }

    private val TABLA_CRC = IntArray(256).also { tabla ->
        for (n in 0..255) {
            var c = n
            repeat(8) {
                c = if (c and 1 != 0) (0xEDB88320.toInt() xor (c ushr 1)) else c ushr 1
            }
            tabla[n] = c
        }
    }

    fun crc32(data: ByteArray): Long {
        var crc = 0xFFFFFFFFL.toInt()
        data.forEach { b -> crc = TABLA_CRC[(crc xor b.toInt()) and 0xFF] xor (crc ushr 8) }
        return (crc xor -1).toLong() and 0xFFFFFFFFL
    }
}

internal class BuilderPiezas {
    private val piezas = ArrayList<ByteArray>()
    private var tamaño = 0

    fun u16(v: Int) { piezas.add(byteArrayOf((v and 0xFF).toByte(), ((v ushr 8) and 0xFF).toByte())); tamaño += 2 }
    fun u32(v: Long) {
        piezas.add(byteArrayOf(
            (v and 0xFF).toByte(), ((v ushr 8) and 0xFF).toByte(),
            ((v ushr 16) and 0xFF).toByte(), ((v ushr 24) and 0xFF).toByte()
        ))
        tamaño += 4
    }
    fun bytes(b: ByteArray) { piezas.add(b); tamaño += b.size }
    fun size(): Int = tamaño
    fun build(): ByteArray {
        val out = ByteArray(tamaño)
        var pos = 0
        piezas.forEach { it.copyInto(out, pos); pos += it.size }
        return out
    }
}
