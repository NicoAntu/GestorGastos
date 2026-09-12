package com.angel.gg.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.angel.gg.domain.model.Gasto
import com.angel.gg.domain.usecase.BuscarGastosUseCase
import com.angel.gg.presentation.i18n.Strings
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

data class BuscadorUiState(
    val query       : String      = "",
    val resultados  : List<Gasto> = emptyList(),
    val buscando    : Boolean     = false,
    val sinResultados: Boolean    = false,
    val mensajeError: String?     = null
)

@OptIn(FlowPreview::class)
class BuscadorViewModel(
    private val buscarGastosUseCase: BuscarGastosUseCase
) : ViewModel() {

    private val _state    = MutableStateFlow(BuscadorUiState())
    val state: StateFlow<BuscadorUiState> = _state

    // Flow interno del texto ingresado para el debounce
    private val _queryFlow = MutableStateFlow("")

    init {
        viewModelScope.launch {
            _queryFlow
                // Espera 400ms después del último caracter antes de buscar
                .debounce(400)
                .distinctUntilChanged()
                .filter { it.length >= 2 }
                .collect { query -> ejecutarBusqueda(query) }
        }
    }

    fun onQueryChange(query: String) {
        _state.value = _state.value.copy(
            query        = query,
            sinResultados = false,
            mensajeError  = null
        )
        _queryFlow.value = query

        // Limpiar resultados si el campo queda vacío
        if (query.isBlank()) {
            _state.value = _state.value.copy(
                resultados = emptyList(),
                buscando   = false
            )
        }
    }

    private suspend fun ejecutarBusqueda(query: String) {
        _state.value = _state.value.copy(buscando = true)

        when (val resultado = buscarGastosUseCase(query)) {
            is BuscarGastosUseCase.Resultado.Exito ->
                _state.value = _state.value.copy(
                    resultados    = resultado.resultados,
                    buscando      = false,
                    sinResultados = false
                )
            is BuscarGastosUseCase.Resultado.SinResultados ->
                _state.value = _state.value.copy(
                    resultados    = emptyList(),
                    buscando      = false,
                    sinResultados = true
                )
            is BuscarGastosUseCase.Resultado.Fallo ->
                _state.value = _state.value.copy(
                    buscando     = false,
                    mensajeError = Strings.t("vm.busquedaMinima")
                )
        }
    }

    fun limpiar() {
        _state.value  = BuscadorUiState()
        _queryFlow.value = ""
    }
}