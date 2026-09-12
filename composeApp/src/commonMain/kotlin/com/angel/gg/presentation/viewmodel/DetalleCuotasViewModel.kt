package com.angel.gg.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.angel.gg.domain.model.Gasto
import com.angel.gg.domain.usecase.EditarCuotasUseCase
import com.angel.gg.domain.usecase.EliminarCuotasUseCase
import com.angel.gg.domain.usecase.ObtenerCronogramaCuotasUseCase
import com.angel.gg.presentation.i18n.Strings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class DetalleCuotasUiState(
    val cronograma      : List<Gasto> = emptyList(),
    val cuotaSeleccionada: Gasto?     = null,
    val cargando        : Boolean     = true,
    val operacionExitosa: Boolean     = false,
    val mensajeError    : String?     = null,
    val mensajeExito    : String?     = null
)

class DetalleCuotasViewModel(
    private val obtenerCronogramaUseCase: ObtenerCronogramaCuotasUseCase,
    private val editarCuotasUseCase     : EditarCuotasUseCase,
    private val eliminarCuotasUseCase   : EliminarCuotasUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(DetalleCuotasUiState())
    val state: StateFlow<DetalleCuotasUiState> = _state

    // Cargar cronograma al abrir la subpantalla (Tarjeta 5, CA1)
    fun cargarCronograma(gastoId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(cargando = true)

            when (val r = obtenerCronogramaUseCase(gastoId)) {
                is ObtenerCronogramaCuotasUseCase.Resultado.Exito -> {
                    _state.value = _state.value.copy(
                        cronograma       = r.cronograma,
                        cuotaSeleccionada = r.cuotaActual,
                        cargando         = false
                    )
                }
                is ObtenerCronogramaCuotasUseCase.Resultado.Fallo -> {
                    _state.value = _state.value.copy(
                        cargando     = false,
                        mensajeError = Strings.t("vm.errCronograma")
                    )
                }
            }
        }
    }

    // Edición con alcance (Tarjeta 5, CA2)
    fun editar(
        monto      : Double,
        descripcion: String,
        categoriaId: String,
        alcance    : EditarCuotasUseCase.AlcanceEdicion
    ) {
        val gastoId = _state.value.cuotaSeleccionada?.id ?: return

        viewModelScope.launch {
            when (val r = editarCuotasUseCase(
                gastoId     = gastoId,
                monto       = monto,
                descripcion = descripcion,
                categoriaId = categoriaId,
                alcance     = alcance
            )) {
                is EditarCuotasUseCase.Resultado.Exito ->
                    _state.value = _state.value.copy(
                        operacionExitosa = true,
                        mensajeExito     = mensajeAlcance(true, alcance.name)
                    )
                is EditarCuotasUseCase.Resultado.Fallo ->
                    _state.value = _state.value.copy(
                        mensajeError = mapearErrorEdicion(r.error)
                    )
            }
        }
    }

    // Eliminación con alcance (Tarjeta 5, CA2)
    fun eliminar(alcance: EliminarCuotasUseCase.AlcanceEliminacion) {
        val gastoId = _state.value.cuotaSeleccionada?.id ?: return

        viewModelScope.launch {
            when (val r = eliminarCuotasUseCase(gastoId = gastoId, alcance = alcance)) {
                is EliminarCuotasUseCase.Resultado.Exito ->
                    _state.value = _state.value.copy(
                        operacionExitosa = true,
                        mensajeExito     = mensajeAlcance(false, alcance.name)
                    )
                is EliminarCuotasUseCase.Resultado.Fallo ->
                    _state.value = _state.value.copy(
                        mensajeError = Strings.t("vm.errNoEliminarCuota")
                    )
            }
        }
    }

    fun limpiarMensajes() {
        _state.value = _state.value.copy(
            mensajeError     = null,
            mensajeExito     = null,
            operacionExitosa = false
        )
    }

    private fun mensajeAlcance(editando: Boolean, alcance: String) = when (alcance) {
        "SOLO_ESTA"     -> Strings.t(if (editando) "vm.editarSoloEsta" else "vm.eliminarSoloEsta")
        "FUTURAS"       -> Strings.t(if (editando) "vm.editarSiguientes" else "vm.eliminarSiguientes")
        "TODA_LA_SERIE" -> Strings.t(if (editando) "vm.serieModificada" else "vm.serieEliminada")
        else            -> Strings.t(if (editando) "vm.editarSoloEsta" else "vm.eliminarSoloEsta")
    }

    private fun mapearErrorEdicion(error: EditarCuotasUseCase.Error) = when (error) {
        EditarCuotasUseCase.Error.MontoInvalido        -> Strings.t("vm.errMontoMayorCero")
        EditarCuotasUseCase.Error.DescripcionVacia     -> Strings.t("vm.errDescripcion")
        EditarCuotasUseCase.Error.CategoriaInexistente -> Strings.t("vm.errCategoria")
        EditarCuotasUseCase.Error.GastoNoEncontrado    -> Strings.t("vm.errCuotaNoEncontrada")
        EditarCuotasUseCase.Error.NoEsUnaCuota         -> Strings.t("vm.errNoEsCuota")
    }
}