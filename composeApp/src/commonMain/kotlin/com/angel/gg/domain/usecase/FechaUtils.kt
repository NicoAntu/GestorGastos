package com.angel.gg.domain.usecase

import java.time.LocalDate

// Devuelve la fecha de hoy como String en formato "YYYY-MM-DD"
fun obtenerFechaHoy(): String = LocalDate.now().toString()

// Extrae año y mes de un String "YYYY-MM-DD"
fun parsearFecha(fecha: String): Pair<Int, Int> {
    val partes = fecha.split("-")
    require(partes.size == 3) { "Formato de fecha inválido: $fecha. Esperado YYYY-MM-DD" }
    return Pair(partes[0].toInt(), partes[1].toInt())
}

// Suma N meses a una fecha "YYYY-MM-DD"
fun sumarMeses(fecha: String, meses: Int): String {
    val partes = fecha.split("-")
    val localDate = LocalDate.of(
        partes[0].toInt(),
        partes[1].toInt(),
        partes[2].toInt()
    )
    return localDate.plusMonths(meses.toLong()).toString()
}

// Genera un UUID simple multiplataforma
fun generarId(): String {
    val chars = "abcdefghijklmnopqrstuvwxyz0123456789"
    fun bloque(n: Int) = (1..n).map { chars.random() }.joinToString("")
    return "${bloque(8)}-${bloque(4)}-${bloque(4)}-${bloque(4)}-${bloque(12)}"
}