package com.angel.gg.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import com.angel.gg.presentation.screen.AppIcons
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.angel.gg.domain.model.Gasto
import com.angel.gg.domain.usecase.EditarCuotasUseCase
import com.angel.gg.domain.usecase.EliminarCuotasUseCase
import com.angel.gg.presentation.i18n.Strings
import com.angel.gg.presentation.i18n.tr
import com.angel.gg.presentation.viewmodel.DetalleCuotasViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetalleCuotasScreen(
    gastoId  : String,
    onVolver : () -> Unit,
    viewModel: DetalleCuotasViewModel = koinInject()
) {
    val state   by viewModel.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    var mostrarDialogoAlcance   by remember { mutableStateOf(false) }
    var accionPendiente         by remember { mutableStateOf<String?>(null) } // "editar" o "eliminar"

    LaunchedEffect(Unit) { viewModel.cargarCronograma(gastoId) }

    LaunchedEffect(state.operacionExitosa) {
        if (state.operacionExitosa) {
            state.mensajeExito?.let { snackbar.showSnackbar(it) }
            viewModel.limpiarMensajes()
            onVolver()
        }
    }

    LaunchedEffect(state.mensajeError) {
        state.mensajeError?.let {
            snackbar.showSnackbar(it)
            viewModel.limpiarMensajes()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(tr("cuotas.detalle")) },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(AppIcons.Volver, contentDescription = tr("comun.volver"))
                    }
                },
                actions = {
                    IconButton(onClick = {
                        accionPendiente      = "editar"
                        mostrarDialogoAlcance = true
                    }) {
                        Icon(AppIcons.Editar, contentDescription = tr("comun.editar"))
                    }
                    IconButton(onClick = {
                        accionPendiente      = "eliminar"
                        mostrarDialogoAlcance = true
                    }) {
                        Icon(
                            AppIcons.Eliminar,
                            contentDescription = tr("comun.eliminar"),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            )
        }
    ) { padding ->

        LazyColumn(
            modifier            = Modifier.fillMaxSize().padding(padding),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                state.cuotaSeleccionada?.let { cuota ->
                    Card(
                        modifier  = Modifier.fillMaxWidth().padding(16.dp),
                        elevation = CardDefaults.cardElevation(4.dp),
                        colors    = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(tr("cuotas.seleccionada"), style = MaterialTheme.typography.labelMedium)
                            Text(cuota.descripcion,    style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${formatMonto(cuota.monto)} — ${cuota.labelCuota}",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    tr("cuotas.cronograma"),
                    style    = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            items(state.cronograma, key = { it.id }) { cuota ->
                CuotaItem(
                    cuota      = cuota,
                    esActual   = cuota.id == state.cuotaSeleccionada?.id
                )
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }

    // Diálogo de selección de alcance (Tarjeta 5, CA2)
    if (mostrarDialogoAlcance) {
        var alcanceSeleccionado by remember { mutableStateOf("SOLO_ESTA") }

        AlertDialog(
            onDismissRequest = { mostrarDialogoAlcance = false },
            title = { Text(if (accionPendiente == "editar") tr("cuotas.queEditar") else tr("cuotas.queEliminar")) },
            text = {
                Column {
                    listOf(
                        "SOLO_ESTA"     to { Strings.t("editar.soloEsta") },
                        "FUTURAS"       to { Strings.t("editar.estaYLasSiguientes") },
                        "TODA_LA_SERIE" to { Strings.t("editar.todaLaSerie") }
                    ).forEach { (valor, label) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier          = Modifier.fillMaxWidth()
                        ) {
                            RadioButton(
                                selected = alcanceSeleccionado == valor,
                                onClick  = { alcanceSeleccionado = valor }
                            )
                            Text(label())
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    mostrarDialogoAlcance = false
                    val cuota = state.cuotaSeleccionada ?: return@TextButton
                    if (accionPendiente == "editar") {
                        val alcance = when (alcanceSeleccionado) {
                            "FUTURAS"       -> EditarCuotasUseCase.AlcanceEdicion.FUTURAS
                            "TODA_LA_SERIE" -> EditarCuotasUseCase.AlcanceEdicion.TODA_LA_SERIE
                            else            -> EditarCuotasUseCase.AlcanceEdicion.SOLO_ESTA
                        }
                        viewModel.editar(cuota.monto, cuota.descripcion, cuota.categoriaId, alcance)
                    } else {
                        val alcance = when (alcanceSeleccionado) {
                            "FUTURAS"       -> EliminarCuotasUseCase.AlcanceEliminacion.FUTURAS
                            "TODA_LA_SERIE" -> EliminarCuotasUseCase.AlcanceEliminacion.TODA_LA_SERIE
                            else            -> EliminarCuotasUseCase.AlcanceEliminacion.SOLO_ESTA
                        }
                        viewModel.eliminar(alcance)
                    }
                }) { Text(tr("comun.confirmar")) }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoAlcance = false }) { Text(tr("comun.cancelar")) }
            }
        )
    }
}

@Composable
private fun CuotaItem(cuota: Gasto, esActual: Boolean) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors   = CardDefaults.cardColors(
            containerColor = if (esActual)
                MaterialTheme.colorScheme.secondaryContainer
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier              = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Column {
                Text(cuota.labelCuota ?: "", style = MaterialTheme.typography.labelMedium)
                Text(cuota.fecha,             style = MaterialTheme.typography.bodySmall)
            }
            Text(
                formatMonto(cuota.monto),
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}