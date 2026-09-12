package com.angel.gg.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.angel.gg.domain.model.Gasto
import com.angel.gg.domain.model.IngresoMensual
import com.angel.gg.domain.repository.GastoRepository
import com.angel.gg.domain.repository.IngresoMensualRepository
import com.angel.gg.domain.usecase.EliminarCuotasUseCase
import com.angel.gg.domain.usecase.GuardarIngresoMensualUseCase
import com.angel.gg.presentation.i18n.Strings
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class HomeUiState(
    val gastos         : List<Gasto>      = emptyList(),
    val ingresoMensual : IngresoMensual?  = null,
    val totalGastado   : Double           = 0.0,
    val anioActual     : Int              = LocalDate.now().year,
    val mesActual      : Int              = LocalDate.now().monthValue,
    val cargando       : Boolean          = true,
    val mensajeError   : String?          = null,
    val mensajeExito   : String?          = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val gastoRepository         : GastoRepository,
    private val ingresoMensualRepository: IngresoMensualRepository,
    private val eliminarCuotasUseCase   : EliminarCuotasUseCase,
    private val guardarIngresoUseCase   : GuardarIngresoMensualUseCase

) : ViewModel() {

    // Fuente de verdad del mes navegado
    private val _mesNavegado = MutableStateFlow(
        Pair(LocalDate.now().year, LocalDate.now().monthValue)
    )

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state

    private val _tabActivo = MutableStateFlow(0)
    val tabActivo: StateFlow<Int> = _tabActivo
    fun setTab(tab: Int) { _tabActivo.value = tab }

    init {
        viewModelScope.launch {
            // flatMapLatest cancela el flow anterior cuando cambia el mes
            _mesNavegado
                .flatMapLatest { (anio, mes) ->
                    combine(
                        gastoRepository.obtenerPorMes(anio, mes),
                        ingresoMensualRepository.obtenerPorMes(anio, mes)
                    ) { gastos, ingreso ->
                        _state.update { current ->
                            current.copy(
                                gastos         = gastos,
                                ingresoMensual = ingreso,
                                totalGastado   = gastos.sumOf { it.monto },
                                anioActual     = anio,
                                mesActual      = mes,
                                cargando       = false
                            )
                        }
                    }
                }
                .collect {}
        }
    }

    fun avanzarMes() {
        _mesNavegado.update { (anio, mes) ->
            if (mes == 12) Pair(anio + 1, 1) else Pair(anio, mes + 1)
        }
    }

    fun retrocederMes() {
        _mesNavegado.update { (anio, mes) ->
            if (mes == 1) Pair(anio - 1, 12) else Pair(anio, mes - 1)
        }
    }

    fun eliminarGasto(gasto: Gasto) {
        viewModelScope.launch {
            if (gasto.esCuota) {
                eliminarCuotasUseCase(
                    gastoId = gasto.id,
                    alcance = EliminarCuotasUseCase.AlcanceEliminacion.SOLO_ESTA
                )
            } else {
                gastoRepository.eliminarUno(gasto.id)
            }
        }
    }

    fun guardarIngreso(monto: Double) {
        viewModelScope.launch {
            val (anio, mes) = _mesNavegado.value
            when (guardarIngresoUseCase(anio = anio, mes = mes, monto = monto)) {
                is GuardarIngresoMensualUseCase.Resultado.Exito ->
                    _state.update { it.copy(mensajeExito = Strings.t("vm.ingresoGuardado")) }
                is GuardarIngresoMensualUseCase.Resultado.Fallo ->
                    _state.update { it.copy(mensajeError = Strings.t("vm.errIngresoNegativo")) }
            }
        }
    }

    fun limpiarMensajes() {
        _state.update { it.copy(mensajeError = null, mensajeExito = null) }
    }
}