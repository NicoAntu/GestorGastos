package com.angel.gg.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.angel.gg.domain.model.Categoria
import com.angel.gg.domain.repository.CategoriaRepository
import com.angel.gg.domain.repository.GastoRepository
import com.angel.gg.domain.usecase.EditarCuotasUseCase
import com.angel.gg.presentation.i18n.Strings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EditarGastoUiState(
    val cargando       : Boolean         = true,
    val guardadoExitoso: Boolean         = false,
    val esCuota        : Boolean         = false,
    val monto          : String          = "",
    val descripcion    : String          = "",
    val categoriaId    : String          = "",
    val fecha          : String          = "",
    val alcanceEdicion : String          = "SOLO_ESTA",
    val categorias     : List<Categoria> = emptyList(),
    val mensajeError   : String?         = null
)

class EditarGastoViewModel(
    private val gastoRepository      : GastoRepository,
    private val categoriaRepository  : CategoriaRepository,
    private val editarCuotasUseCase  : EditarCuotasUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(EditarGastoUiState())
    val state: StateFlow<EditarGastoUiState> = _state

    private var gastoId: String = ""

    fun cargar(id: String) {
        gastoId = id
        viewModelScope.launch {
            // Cargar categorías
            categoriaRepository.obtenerTodas().collect { cats ->
                _state.update { it.copy(categorias = cats) }
            }
        }
        viewModelScope.launch {
            val gasto = gastoRepository.obtenerPorId(id)
            if (gasto == null) {
                _state.update { it.copy(
                    cargando     = false,
                    mensajeError = Strings.t("vm.errGastoNoEncontrado")
                )}
                return@launch
            }
            _state.update { it.copy(
                cargando    = false,
                esCuota     = gasto.esCuota,
                monto       = gasto.monto.toString(),
                descripcion = gasto.descripcion,
                categoriaId = gasto.categoriaId,
                fecha       = gasto.fecha
            )}
        }
    }

    fun onMontoChange(v: String)      { _state.update { it.copy(monto       = v) } }
    fun onDescripcionChange(v: String){ _state.update { it.copy(descripcion = v) } }
    fun onCategoriaChange(v: String)  { _state.update { it.copy(categoriaId = v) } }
    fun onFechaChange(v: String)      { _state.update { it.copy(fecha        = v) } }
    fun onAlcanceChange(v: String)    { _state.update { it.copy(alcanceEdicion = v) } }

    fun guardar() {
        val s = _state.value
        val monto = s.monto.toDoubleOrNull() ?: run {
            _state.update { it.copy(mensajeError = Strings.t("vm.errMontoValido")) }
            return
        }

        viewModelScope.launch {
            if (s.esCuota) {
                val alcance = when (s.alcanceEdicion) {
                    "FUTURAS"       -> EditarCuotasUseCase.AlcanceEdicion.FUTURAS
                    "TODA_LA_SERIE" -> EditarCuotasUseCase.AlcanceEdicion.TODA_LA_SERIE
                    else            -> EditarCuotasUseCase.AlcanceEdicion.SOLO_ESTA
                }
                when (val r = editarCuotasUseCase(
                    gastoId     = gastoId,
                    monto       = monto,
                    descripcion = s.descripcion,
                    categoriaId = s.categoriaId,
                    alcance     = alcance
                )) {
                    is EditarCuotasUseCase.Resultado.Exito ->
                        _state.update { it.copy(guardadoExitoso = true) }
                    is EditarCuotasUseCase.Resultado.Fallo ->
                        _state.update { it.copy(mensajeError = Strings.t("vm.errEditar", r.error)) }
                }
            } else {
                val gasto = gastoRepository.obtenerPorId(gastoId) ?: return@launch
                gastoRepository.actualizarUno(
                    gasto.copy(
                        monto       = monto,
                        descripcion = s.descripcion,
                        categoriaId = s.categoriaId,
                        fecha       = s.fecha
                    )
                )
                _state.update { it.copy(guardadoExitoso = true) }
            }
        }
    }

    fun limpiarMensajes() {
        _state.update { it.copy(mensajeError = null, guardadoExitoso = false) }
    }
}