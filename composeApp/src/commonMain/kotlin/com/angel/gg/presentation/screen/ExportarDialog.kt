package com.angel.gg.presentation.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.angel.gg.domain.model.ExportarAlcance
import com.angel.gg.presentation.AppTheme
import com.angel.gg.presentation.i18n.tr
import com.angel.gg.presentation.viewmodel.ExportarViewModel
import java.time.LocalDate
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportarDialog(
    onDismiss: () -> Unit,
    viewModel: ExportarViewModel = koinInject()
) {
    val estado by viewModel.state.collectAsState()

    var alcance by remember { mutableStateOf(ExportarAlcance.MES_ACTUAL) }
    var desde by remember { mutableStateOf<LocalDate?>(null) }
    var hasta by remember { mutableStateOf<LocalDate?>(null) }
    var editarCampo by remember { mutableStateOf<Boolean?>(null) } // true=desde, false=hasta

    // Una vez generados los bytes, disparar el guardado (JFileChooser o SAF)
    LaunchedEffect(estado.listoParaGuardar) {
        if (estado.listoParaGuardar) viewModel.guardar()
    }

    // Cerrar el diálogo al confirmar el guardado
    LaunchedEffect(estado.mensaje) {
        val m = estado.mensaje
        if (m != null && !estado.esError) onDismiss()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppTheme.CardDark,
        title = { Text(tr("exportar.titulo"), color = AppTheme.TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ExportarAlcance.entries.forEach { opcion ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = alcance == opcion,
                            onClick = { alcance = opcion },
                            colors = RadioButtonDefaults.colors(selectedColor = AppTheme.Indigo)
                        )
                        Text(
                            when (opcion) {
                                ExportarAlcance.MES_ACTUAL -> tr("exportar.mesActual")
                                ExportarAlcance.ANIO_ACTUAL -> tr("exportar.anioActual")
                                ExportarAlcance.TODO -> tr("exportar.todo")
                                ExportarAlcance.PERSONALIZADO -> tr("exportar.personalizado")
                            },
                            color = AppTheme.TextPrimary,
                            fontSize = 15.sp
                        )
                    }
                }

                if (alcance == ExportarAlcance.PERSONALIZADO) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        FechaCampo(
                            etiqueta = tr("exportar.desde"), valor = desde,
                            modifier = Modifier.weight(1f)
                        ) { editarCampo = true }
                        FechaCampo(
                            etiqueta = tr("exportar.hasta"), valor = hasta,
                            modifier = Modifier.weight(1f)
                        ) { editarCampo = false }
                    }
                }

                if (estado.exportando) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(8.dp),
                            color = AppTheme.Indigo
                        )
                    }
                }

                estado.mensaje?.let {
                    if (estado.esError) {
                        Text(it, color = AppTheme.Red, fontSize = 13.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !estado.exportando && alcanceValido(alcance, desde, hasta),
                onClick = { viewModel.exportar(alcance, desde, hasta) }
            ) {
                Text(tr("exportar.boton"), color = if (estado.exportando) AppTheme.TextSubtle else AppTheme.Indigo)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(tr("comun.cancelar"), color = AppTheme.TextMuted)
            }
        }
    )

    if (alcance == ExportarAlcance.PERSONALIZADO && editarCampo != null) {
        val campo = editarCampo
        val fechaInicial = if (campo == true) desde else hasta
        val millisInicial = fechaInicial?.atStartOfDay(java.time.ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
        val state = rememberDatePickerState(initialSelectedDateMillis = millisInicial)
        DatePickerDialog(
            onDismissRequest = { editarCampo = null },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        val fecha = java.time.Instant.ofEpochMilli(millis)
                            .atZone(java.time.ZoneOffset.UTC)
                            .toLocalDate()
                        if (campo == true) desde = fecha else hasta = fecha
                    }
                    editarCampo = null
                }) { Text("OK", color = AppTheme.Indigo) }
            },
            dismissButton = {
                TextButton(onClick = { editarCampo = null }) {
                    Text(tr("comun.cancelar"), color = AppTheme.TextMuted)
                }
            }
        ) {
            DatePicker(state = state)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FechaCampo(
    etiqueta: String,
    valor: LocalDate?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(modifier = modifier) {
        Text(etiqueta, color = AppTheme.TextMuted, fontSize = 12.sp)
        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(4.dp),
            color = AppTheme.CardDark,
            border = BorderStroke(1.dp, AppTheme.BorderColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    valor?.toString() ?: tr("registrar.seleccionarFecha"),
                    color = if (valor == null) AppTheme.TextSubtle else AppTheme.TextPrimary,
                    fontSize = 14.sp
                )
                Icon(
                    AppIcons.Calendario,
                    contentDescription = tr("registrar.seleccionarFecha"),
                    tint = AppTheme.TextMuted
                )
            }
        }
    }
}

private fun alcanceValido(
    alcance: ExportarAlcance,
    desde: LocalDate?,
    hasta: LocalDate?
): Boolean = when (alcance) {
    ExportarAlcance.PERSONALIZADO -> desde != null && hasta != null && !hasta.isBefore(desde)
    else -> true
}