package com.angel.gg.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.angel.gg.domain.model.ExportarAlcance
import com.angel.gg.domain.model.ExportarContenido
import com.angel.gg.domain.model.ExportarDatos
import com.angel.gg.domain.model.HojaExcel
import com.angel.gg.domain.model.IngresoMensual
import com.angel.gg.domain.usecase.ExportarGastosUseCase
import com.angel.gg.export.ExportarGuardado
import com.angel.gg.export.XlsxGenerador
import com.angel.gg.presentation.i18n.Strings
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ExportarUiState(
    val exportando: Boolean = false,
    val listoParaGuardar: Boolean = false,
    val bytes: ByteArray? = null,
    val nombreArchivo: String? = null,
    val mensaje: String? = null,
    val esError: Boolean = false
)

class ExportarViewModel(
    private val exportarGastosUseCase: ExportarGastosUseCase,
    private val exportarGuardado: ExportarGuardado
) : ViewModel() {

    private val _state = MutableStateFlow(ExportarUiState())
    val state: StateFlow<ExportarUiState> = _state

    fun exportar(alcance: ExportarAlcance, desde: LocalDate?, hasta: LocalDate?) {
        viewModelScope.launch {
            _state.update { it.copy(exportando = true, mensaje = null, bytes = null, listoParaGuardar = false) }
            runCatching {
                val datos = exportarGastosUseCase.ejecutar(alcance, desde, hasta)
                if (datos.gastos.isEmpty()) {
                    _state.update {
                        it.copy(exportando = false, mensaje = Strings.t("exportar.sinDatos"), esError = true)
                    }
                } else {
                    val contenido = ExportarContenido(
                        nombreArchivo = archivoDe(alcance, datos),
                        hojas = hojasDe(datos)
                    )
                    val bytes = XlsxGenerador.generar(contenido)
                    _state.update {
                        it.copy(
                            exportando = false, bytes = bytes,
                            nombreArchivo = contenido.nombreArchivo, listoParaGuardar = true
                        )
                    }
                }
            }.onFailure {
                _state.update { it.copy(exportando = false, mensaje = Strings.t("exportar.error"), esError = true) }
            }
        }
    }

    private fun hojasDe(datos: ExportarDatos): List<HojaExcel> =
        datos.gastos
            .groupBy { gasto ->
                val fecha = LocalDate.parse(gasto.fecha)
                fecha.year to fecha.monthValue
            }
            .toSortedMap(compareBy { it.first * 100 + it.second })
            .map { (anioMes, gastosMes) ->
                HojaExcel(
                    nombre = "${Strings.mes(anioMes.second)} ${anioMes.first}",
                    gastos = gastosMes,
                    ingresoTotal = ingresoDe(datos.ingresos, anioMes.first, anioMes.second)
                )
            }

    private fun ingresoDe(ingresos: List<IngresoMensual>, anio: Int, mes: Int): Double =
        ingresos.firstOrNull { it.anio == anio && it.mes == mes }?.monto ?: 0.0

    fun guardar() {
        val actual = _state.value
        val bytes = actual.bytes ?: return
        val nombre = actual.nombreArchivo ?: return
        viewModelScope.launch {
            val ok = exportarGuardado.guardar(nombre, bytes)
            _state.update {
                it.copy(
                    listoParaGuardar = false, bytes = null, nombreArchivo = null,
                    mensaje = if (ok) Strings.t("exportar.exito") else Strings.t("exportar.error"),
                    esError = !ok
                )
            }
        }
    }

    fun limpiarMensaje() {
        _state.update { it.copy(mensaje = null, esError = false) }
    }

    private fun archivoDe(alcance: ExportarAlcance, datos: ExportarDatos): String =
        when (alcance) {
            ExportarAlcance.MES_ACTUAL -> "Gastos ${Strings.mes(datos.desde.monthValue)} ${datos.desde.year}.xlsx"
            ExportarAlcance.ANIO_ACTUAL -> "Gastos ${datos.desde.year}.xlsx"
            ExportarAlcance.TODO -> "Gastos.xlsx"
            ExportarAlcance.PERSONALIZADO -> "Gastos ${datos.desde} a ${datos.hasta}.xlsx"
        }
}
