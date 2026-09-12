package com.angel.gg.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.angel.gg.domain.model.ResumenMes
import com.angel.gg.domain.repository.GastoRepository
import com.angel.gg.domain.repository.IngresoMensualRepository
import com.angel.gg.domain.usecase.ObtenerResumenMesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

class EstadisticasViewModel(
    private val gastoRepository         : GastoRepository,
    private val ingresoMensualRepository: IngresoMensualRepository,
    private val obtenerResumenMesUseCase : ObtenerResumenMesUseCase
) : ViewModel() {

    data class CuotaSerie(
        val idPadre       : String,
        val descripcion   : String,
        val cuotaActual   : Int,
        val cuotaTotal    : Int,
        val montoPorCuota : Double,
        val montoRestante : Double,
        val primerGastoId : String
    )

    data class UiState(
        val porcentajeConsumo : Float              = 0f,
        val totalGastado      : Double             = 0.0,
        val ingresoMensual    : Double             = 0.0,
        val promedioAnual     : Double             = 0.0,
        val gastosPorCategoria: List<Triple<String, Double, String>> = emptyList(),
        val cuotasSeries      : List<CuotaSerie>  = emptyList(),
        val cargando          : Boolean            = true
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state

    private val hoy = LocalDate.now()

    init {
        cargarResumenMes()
        cargarPromedioAnual()
        cargarCuotasSeries()
    }

    private fun cargarResumenMes() {
        viewModelScope.launch {
            obtenerResumenMesUseCase(hoy.year, hoy.monthValue).collect { resumen ->
                _state.update { it.copy(
                    totalGastado       = resumen.totalGastado,
                    ingresoMensual     = resumen.ingresoMensual,
                    porcentajeConsumo  = (resumen.porcentajeConsumido / 100).coerceIn(0.0, 1.0).toFloat(),
                    gastosPorCategoria = resumen.gastosPorCategoria.filter { gc -> gc.total > 0 }.map { gc ->
                        Triple(gc.categoriaNombre, gc.total, gc.categoriaColor)
                    },
                    cargando = false
                )}
            }
        }
    }

    private fun cargarPromedioAnual() {
        viewModelScope.launch {
            val totalesPorMes = (1..12).mapNotNull { mes ->
                try {
                    gastoRepository.totalPorMes(hoy.year, mes).takeIf { it > 0 }
                } catch (e: Exception) { null }
            }
            val promedio = if (totalesPorMes.isNotEmpty()) {
                totalesPorMes.sum() / totalesPorMes.size
            } else 0.0
            _state.update { it.copy(promedioAnual = promedio) }
        }
    }

    private fun cargarCuotasSeries() {
        viewModelScope.launch {
            gastoRepository.obtenerPorMes(hoy.year, hoy.monthValue).collect { gastos ->
                val cuotasDelMes = gastos.filter { it.esCuota && it.idPadre != null }
                val series = cuotasDelMes
                    .groupBy { it.idPadre!! }
                    .map { (idPadre, lista) ->
                        val primero      = lista.first()
                        val cuotaActual  = primero.cuotaActual ?: 1
                        val cuotaTotal   = primero.cuotaTotal  ?: 1
                        val monto        = primero.monto
                        val restante     = (cuotaTotal - cuotaActual) * monto
                        CuotaSerie(
                            idPadre       = idPadre,
                            descripcion   = primero.descripcion,
                            cuotaActual   = cuotaActual,
                            cuotaTotal    = cuotaTotal,
                            montoPorCuota = monto,
                            montoRestante = restante,
                            primerGastoId = primero.id
                        )
                    }
                _state.update { it.copy(cuotasSeries = series) }
            }
        }
    }
}