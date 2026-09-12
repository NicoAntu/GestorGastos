package com.angel.gg.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.angel.gg.domain.model.ResumenMes
import com.angel.gg.domain.usecase.ObtenerResumenMesUseCase
import com.angel.gg.presentation.i18n.Strings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

data class DashboardUiState(
    val resumen    : ResumenMes? = null,
    val cargando   : Boolean     = true,
    val mensajeError: String?    = null
)

class DashboardViewModel(
    private val obtenerResumenMesUseCase: ObtenerResumenMesUseCase
) : ViewModel() {

    private val hoy = LocalDate.now()

    private val _state = MutableStateFlow(DashboardUiState())
    val state: StateFlow<DashboardUiState> = _state

    init {
        cargarResumen(hoy.year, hoy.monthValue)
    }

    fun cargarResumen(anio: Int, mes: Int) {
        viewModelScope.launch {
            _state.value = _state.value.copy(cargando = true)
            try {
                // Flow reactivo — se actualiza automáticamente (Tarjeta 7)
                obtenerResumenMesUseCase(anio, mes).collect { resumen ->
                    _state.value = _state.value.copy(
                        resumen  = resumen,
                        cargando = false
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    cargando     = false,
                    mensajeError = Strings.t("vm.errResumen")
                )
            }
        }
    }
}