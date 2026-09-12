package com.angel.gg.domain.model

data class ResumenMes(
    val anio: Int,
    val mes: Int,
    val totalGastado: Double,
    val ingresoMensual: Double,
    val gastosPorCategoria: List<GastoCategoria>
) {
    val porcentajeConsumido: Double get() =
        if (ingresoMensual > 0) (totalGastado / ingresoMensual) * 100.0 else 0.0

    val balanceDisponible: Double get() = ingresoMensual - totalGastado
}

data class GastoCategoria(
    val categoriaId: String,
    val categoriaNombre: String,
    val categoriaColor: String,
    val total: Double,
    val porcentaje: Double
)
