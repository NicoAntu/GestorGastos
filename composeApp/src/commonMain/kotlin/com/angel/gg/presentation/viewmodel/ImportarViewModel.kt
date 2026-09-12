package com.angel.gg.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.angel.gg.domain.model.ImportacionResultado
import com.angel.gg.domain.usecase.ImportarGastosUseCase
import com.angel.gg.export.ImportarLectura
import com.angel.gg.export.ImportarXlsx
import com.angel.gg.presentation.i18n.Strings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ImportarUiState(
    val importando: Boolean = false,
    val mensaje: String? = null,
    val esError: Boolean = false
)

class ImportarViewModel(
    private val importarLectura: ImportarLectura,
    private val importarGastosUseCase: ImportarGastosUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(ImportarUiState())
    val state: StateFlow<ImportarUiState> = _state

    fun importar() {
        viewModelScope.launch {
            _state.update { it.copy(importando = true, mensaje = null) }
            runCatching {
                val bytes = importarLectura.leer() ?: run {
                    _state.update { it.copy(importando = false) }
                    return@launch
                }
                when (val lectura = ImportarXlsx.leer(bytes)) {
                    is ImportarXlsx.ResultadoLectura.ArchivoInvalido ->
                        _state.update {
                            it.copy(importando = false, mensaje = Strings.t("importar.archivoNoValido"), esError = true)
                        }
                    is ImportarXlsx.ResultadoLectura.Ok ->
                        when (val resultado = importarGastosUseCase(lectura.datos)) {
                            is ImportacionResultado.SinGastos ->
                                _state.update {
                                    it.copy(importando = false, mensaje = Strings.t("importar.sinGastos"), esError = true)
                                }
                            is ImportacionResultado.Exito ->
                                _state.update {
                                    it.copy(
                                        importando = false,
                                        mensaje = Strings.t(
                                            "importar.exito",
                                            resultado.resumen.nuevos,
                                            resultado.resumen.omitidos,
                                            resultado.resumen.categoriasCreadas,
                                            resultado.resumen.ingresosRestaurados
                                        ),
                                        esError = false
                                    )
                                }
                        }
                }
            }.onFailure {
                _state.update { it.copy(importando = false, mensaje = Strings.t("importar.error"), esError = true) }
            }
        }
    }

    fun limpiarMensaje() {
        _state.update { it.copy(mensaje = null, esError = false) }
    }
}
