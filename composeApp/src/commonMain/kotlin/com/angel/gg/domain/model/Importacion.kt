package com.angel.gg.domain.model

import java.time.LocalDate

data class FilaImportada(
    val monto: Double,
    val descripcion: String,
    val categoriaNombre: String,
    val fecha: LocalDate,
    val esCuota: Boolean
)

data class ResultadoImportacion(
    val filas: List<FilaImportada>,
    val ingresosPorMes: List<IngresoMensual>
)

data class ResumenImportacion(
    val nuevos: Int,
    val omitidos: Int,
    val categoriasCreadas: Int,
    val ingresosRestaurados: Int
)

sealed interface ImportacionResultado {
    data class Exito(val resumen: ResumenImportacion) : ImportacionResultado
    data object SinGastos : ImportacionResultado
}