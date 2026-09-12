package com.angel.gg.export

import java.io.ByteArrayInputStream
import java.util.zip.Inflater
import java.util.zip.InflaterInputStream

actual fun inflarDeflate(bytes: ByteArray): ByteArray =
    InflaterInputStream(ByteArrayInputStream(bytes), Inflater(true)).use { it.readBytes() }