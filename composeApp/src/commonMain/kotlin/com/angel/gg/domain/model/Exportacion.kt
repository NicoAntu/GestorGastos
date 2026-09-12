package com.angel.gg.domain.model

import java.time.LocalDate

enum class ExportarAlcance { MES_ACTUAL, ANIO_ACTUAL, TODO, PERSONALIZADO }

data class ExportarDatos(
    val gastos: List<Gasto>,
    val ingresos: List<IngresoMensual>,
    val desde: LocalDate,
    val hasta: LocalDate,
    val alcance: ExportarAlcance
)

data class HojaExcel(
    val nombre: String,
    val gastos: List<Gasto>,
    val ingresoTotal: Double
)

data class ExportarContenido(
    val nombreArchivo: String,
    val hojas: List<HojaExcel>
)
