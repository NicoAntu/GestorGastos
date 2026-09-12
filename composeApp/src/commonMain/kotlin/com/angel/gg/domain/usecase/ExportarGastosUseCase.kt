package com.angel.gg.domain.usecase

import com.angel.gg.domain.model.ExportarAlcance
import com.angel.gg.domain.model.ExportarDatos
import com.angel.gg.domain.repository.GastoRepository
import com.angel.gg.domain.repository.IngresoMensualRepository
import java.time.LocalDate

class ExportarGastosUseCase(
    private val gastoRepository: GastoRepository,
    private val ingresoMensualRepository: IngresoMensualRepository
) {
    suspend fun ejecutar(
        alcance: ExportarAlcance,
        desde: LocalDate?,
        hasta: LocalDate?
    ): ExportarDatos {
        val hoy = LocalDate.now()
        val (fDesde, fHasta) = when (alcance) {
            ExportarAlcance.MES_ACTUAL -> {
                val m = hoy.withDayOfMonth(1)
                Pair(m, m.plusMonths(1).minusDays(1))
            }
            ExportarAlcance.ANIO_ACTUAL ->
                Pair(LocalDate.of(hoy.year, 1, 1), LocalDate.of(hoy.year, 12, 31))
            ExportarAlcance.TODO ->
                Pair(LocalDate.of(1900, 1, 1), LocalDate.of(9999, 12, 31))
            ExportarAlcance.PERSONALIZADO -> {
                val d = requireNotNull(desde) { "Rango personalizado sin fecha desde" }
                val h = requireNotNull(hasta) { "Rango personalizado sin fecha hasta" }
                require(!h.isBefore(d)) { "desde debe ser <= hasta" }
                Pair(d, h)
            }
        }

        val gastos = gastoRepository.obtenerPorRango(fDesde.toString(), fHasta.toString())
        val desdeKey = fDesde.year * 100L + fDesde.monthValue
        val hastaKey = fHasta.year * 100L + fHasta.monthValue
        val ingresos = ingresoMensualRepository.obtenerEnRango(desdeKey, hastaKey)

        return ExportarDatos(
            gastos = gastos,
            ingresos = ingresos,
            desde = fDesde,
            hasta = fHasta,
            alcance = alcance
        )
    }
}
