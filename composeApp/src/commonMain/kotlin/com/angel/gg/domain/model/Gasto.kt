package com.angel.gg.domain.model

data class Gasto(
    val id: String,
    val monto: Double,
    val descripcion: String,
    val categoriaId: String,
    val categoriaNombre: String,
    val categoriaColor: String,
    val categoriaIcono: String,
    val fecha: String,
    val anio: Int,
    val mes: Int,
    // Null si no es una compra en cuotas
    val cuotaActual: Int? = null,
    val cuotaTotal: Int? = null,
    val idPadre: String? = null
) {
    val esCuota: Boolean get() = idPadre != null
    val labelCuota: String? get() =
        if (esCuota) "Cuota $cuotaActual/$cuotaTotal" else null
}