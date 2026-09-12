package com.angel.gg.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.angel.gg.domain.model.Categoria
import com.angel.gg.domain.repository.CategoriaRepository
import com.angel.gg.domain.usecase.GenerarCuotasUseCase
import com.angel.gg.domain.usecase.RegistrarGastoUseCase
import com.angel.gg.presentation.i18n.Strings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class RegistrarGastoUiState(
    val categorias     : List<Categoria> = emptyList(),
    val cargando       : Boolean         = false,
    val guardadoExitoso: Boolean         = false,
    val mensajeError   : String?         = null,
    // Campos del formulario
    val monto          : String          = "",
    val descripcion    : String          = "",
    val categoriaId    : String          = "",
    val fecha          : String          = "",
    val esCuota        : Boolean         = false,
    val cantCuotas     : String          = "2"
)

class RegistrarGastoViewModel(
    private val registrarGastoUseCase: RegistrarGastoUseCase,
    private val generarCuotasUseCase : GenerarCuotasUseCase,
    private val categoriaRepository  : CategoriaRepository
) : ViewModel() {

    private val _state = MutableStateFlow(RegistrarGastoUiState())
    val state: StateFlow<RegistrarGastoUiState> = _state

    init {
        cargarCategorias()
    }

    private fun cargarCategorias() {
        viewModelScope.launch {
            categoriaRepository.obtenerTodas().collect { categorias ->
                _state.value = _state.value.copy(categorias = categorias)
            }
        }
    }

    // Actualización de campos del formulario
    fun onMontoChange(valor: String)      { _state.value = _state.value.copy(monto       = valor) }
    fun onDescripcionChange(valor: String){ _state.value = _state.value.copy(descripcion = valor) }
    fun onCategoriaChange(id: String)     { _state.value = _state.value.copy(categoriaId = id)    }
    fun onFechaSeleccionada(fechaStr: String) { _state.value = _state.value.copy(fecha = fechaStr) }
    fun onCantCuotasChange(valor: String) { _state.value = _state.value.copy(cantCuotas  = valor) }

    fun onToggleCuota(esCuota: Boolean) {
        _state.value = _state.value.copy(esCuota = esCuota)
    }

    fun guardar() {
        val s = _state.value
        val monto = s.monto.toDoubleOrNull() ?: run {
            _state.value = s.copy(mensajeError = Strings.t("vm.errMontoValido"))
            return
        }

        _state.value = s.copy(cargando = true, mensajeError = null)

        viewModelScope.launch {
            if (s.esCuota) {
                // Registro en cuotas (Tarjeta 4)
                val cantCuotas = s.cantCuotas.toIntOrNull() ?: run {
                    _state.value = _state.value.copy(
                        cargando     = false,
                        mensajeError = Strings.t("vm.errCantCuotas")
                    )
                    return@launch
                }

                val resultado = generarCuotasUseCase(
                    monto       = monto,
                    descripcion = s.descripcion,
                    categoriaId = s.categoriaId,
                    fechaInicio = s.fecha.ifBlank { null },
                    cantCuotas  = cantCuotas
                )

                when (resultado) {
                    is GenerarCuotasUseCase.Resultado.Exito ->
                        _state.value = _state.value.copy(
                            cargando        = false,
                            guardadoExitoso = true
                        )
                    is GenerarCuotasUseCase.Resultado.Fallo ->
                        _state.value = _state.value.copy(
                            cargando     = false,
                            mensajeError = mapearErrorCuota(resultado.error)
                        )
                }
            } else {
                // Registro simple (Tarjeta 1)
                val resultado = registrarGastoUseCase(
                    monto       = monto,
                    descripcion = s.descripcion,
                    categoriaId = s.categoriaId,
                    fecha       = s.fecha.ifBlank { null }
                )

                when (resultado) {
                    is RegistrarGastoUseCase.Resultado.Exito ->
                        _state.value = _state.value.copy(
                            cargando        = false,
                            guardadoExitoso = true
                        )
                    is RegistrarGastoUseCase.Resultado.Fallo ->
                        _state.value = _state.value.copy(
                            cargando     = false,
                            mensajeError = mapearErrorGasto(resultado.error)
                        )
                }
            }
        }
    }

    fun inicializarFecha(anio: Int, mes: Int) {
        if (_state.value.fecha.isBlank()) {
            _state.value = _state.value.copy(
                fecha = "$anio-${mes.toString().padStart(2, '0')}-01"
            )
        }
    }

    fun resetearFormulario() {
        _state.value = RegistrarGastoUiState(
            categorias = _state.value.categorias
        )
    }

    fun limpiarMensajes() {
        _state.value = _state.value.copy(
            mensajeError    = null,
            guardadoExitoso = false
        )
    }

    private fun mapearErrorGasto(error: RegistrarGastoUseCase.Error) = when (error) {
        RegistrarGastoUseCase.Error.MontoInvalido        -> Strings.t("vm.errMontoMayorCero")
        RegistrarGastoUseCase.Error.DescripcionVacia     -> Strings.t("vm.errDescripcion")
        RegistrarGastoUseCase.Error.CategoriaInexistente -> Strings.t("vm.errCategoria")
    }

    private fun mapearErrorCuota(error: GenerarCuotasUseCase.Error) = when (error) {
        GenerarCuotasUseCase.Error.MontoInvalido          -> Strings.t("vm.errMontoMayorCero")
        GenerarCuotasUseCase.Error.DescripcionVacia       -> Strings.t("vm.errDescripcion")
        GenerarCuotasUseCase.Error.CategoriaInexistente   -> Strings.t("vm.errCategoria")
        GenerarCuotasUseCase.Error.CantidadCuotasInvalida -> "La cantidad de cuotas debe ser al menos 2"
    }
}