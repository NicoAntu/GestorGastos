package com.angel.gg.domain.usecase

import com.angel.gg.domain.model.GastoCategoria
import com.angel.gg.domain.model.ResumenMes
import com.angel.gg.domain.repository.GastoRepository
import com.angel.gg.domain.repository.IngresoMensualRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class ObtenerResumenMesUseCase(
    private val gastoRepository        : GastoRepository,
    private val ingresoMensualRepository: IngresoMensualRepository
) {
    // Devuelve Flow para que el dashboard se actualice en tiempo real
    // cada vez que se agrega, edita o elimina un gasto del mes
    operator fun invoke(anio: Int, mes: Int): Flow<ResumenMes> =
        combine(
            // Flow 1: lista de gastos del mes agrupados por categoría
            gastoRepository.obtenerGastosPorCategoriaMes(anio, mes),
            // Flow 2: ingreso mensual definido para ese mes
            ingresoMensualRepository.obtenerPorMes(anio, mes)
        ) { gastosCategoria, ingresoMensual ->

            val ingresoMonto = ingresoMensual?.monto ?: 0.0
            val totalGastado = gastosCategoria.sumOf { it.total }

            // Calcular el porcentaje de cada categoría respecto al ingreso
            // Si no hay ingreso definido, el porcentaje se calcula sobre el total gastado
            val denominador = if (ingresoMonto > 0) ingresoMonto else totalGastado

            val gastosPorCategoria = gastosCategoria.map { item ->
                GastoCategoria(
                    categoriaId     = item.categoriaId,
                    categoriaNombre = item.categoriaNombre,
                    categoriaColor  = item.categoriaColor,
                    total           = item.total,
                    porcentaje      = if (denominador > 0)
                        redondear((item.total / denominador) * 100)
                    else 0.0
                )
            }

            ResumenMes(
                anio                = anio,
                mes                 = mes,
                totalGastado        = redondear(totalGastado),
                ingresoMensual      = ingresoMonto,
                gastosPorCategoria  = gastosPorCategoria
            )
        }

    private fun redondear(valor: Double): Double =
        kotlin.math.round(valor * 100) / 100.0
}