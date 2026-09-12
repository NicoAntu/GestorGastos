package com.angel.gg.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.angel.gg.domain.model.Categoria
import com.angel.gg.domain.repository.CategoriaRepository
import com.angel.gg.domain.usecase.GestionarCategoriaUseCase
import com.angel.gg.presentation.i18n.Strings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class CategoriaUiState(
    val categorias      : List<Categoria> = emptyList(),
    val cargando        : Boolean         = true,
    val mensajeError    : String?         = null,
    val mensajeExito    : String?         = null,
    val guardadoExitoso : Boolean         = false
)

class CategoriaViewModel(
    private val gestionarCategoriaUseCase: GestionarCategoriaUseCase,
    private val categoriaRepository      : CategoriaRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CategoriaUiState())
    val state: StateFlow<CategoriaUiState> = _state

    init {
        observarCategorias()
    }

    private fun observarCategorias() {
        viewModelScope.launch {
            categoriaRepository.obtenerTodas().collect { lista ->
                _state.value = _state.value.copy(
                    categorias = lista,
                    cargando   = false
                )
            }
        }
    }

    fun crear(
        nombre     : String,
        descripcion: String?,
        color      : String,
        icono      : String,
        fijada     : Boolean
    ) {
        viewModelScope.launch {
            when (val r = gestionarCategoriaUseCase.crear(nombre, descripcion, color, icono, fijada)) {
                is GestionarCategoriaUseCase.Resultado.Exito ->
                    _state.value = _state.value.copy(
                        mensajeExito    = Strings.t("vm.catCreada"),
                        guardadoExitoso = true
                    )
                is GestionarCategoriaUseCase.Resultado.Fallo ->
                    _state.value = _state.value.copy(
                        mensajeError = mapearError(r.error)
                    )
                else -> Unit
            }
        }
    }

    fun editar(
        id         : String,
        nombre     : String,
        descripcion: String?,
        color      : String,
        icono      : String,
        fijada     : Boolean
    ) {
        viewModelScope.launch {
            when (val r = gestionarCategoriaUseCase.editar(id, nombre, descripcion, color, icono, fijada)) {
                is GestionarCategoriaUseCase.Resultado.ExitoSinRetorno ->
                    _state.value = _state.value.copy(
                        mensajeExito    = Strings.t("vm.catActualizada"),
                        guardadoExitoso = true
                    )
                is GestionarCategoriaUseCase.Resultado.Fallo ->
                    _state.value = _state.value.copy(
                        mensajeError = mapearError(r.error)
                    )
                else -> Unit
            }
        }
    }

    // Drag & drop — recibe la lista reordenada (Tarjeta 2, CA3)
    fun reordenar(categoriasReordenadas: List<Categoria>) {
        viewModelScope.launch {
            gestionarCategoriaUseCase.reordenar(categoriasReordenadas)
        }
    }

    fun eliminar(id: String) {
        viewModelScope.launch {
            when (val r = gestionarCategoriaUseCase.eliminar(id)) {
                is GestionarCategoriaUseCase.Resultado.ExitoSinRetorno ->
                    _state.value = _state.value.copy(
                        mensajeExito = Strings.t("vm.catEliminada")
                    )
                is GestionarCategoriaUseCase.Resultado.Fallo ->
                    _state.value = _state.value.copy(
                        mensajeError = mapearError(r.error)
                    )
                else -> Unit
            }
        }
    }

    fun limpiarMensajes() {
        _state.value = _state.value.copy(
            mensajeError    = null,
            mensajeExito    = null,
            guardadoExitoso = false
        )
    }

    private fun mapearError(error: GestionarCategoriaUseCase.Error) = when (error) {
        GestionarCategoriaUseCase.Error.NombreVacio          -> Strings.t("vm.errNombreVacio")
        GestionarCategoriaUseCase.Error.ColorInvalido        -> Strings.t("vm.errColor")
        GestionarCategoriaUseCase.Error.CategoriaInexistente -> Strings.t("vm.errCatInexistente")
        is GestionarCategoriaUseCase.Error.CategoriaConGastos ->
            Strings.t("vm.errCatConGastos", error.cantidad)
    }
}