package com.angel.gg.presentation.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.angel.gg.presentation.AppTheme
import com.angel.gg.presentation.i18n.Strings
import com.angel.gg.presentation.i18n.tr
import com.angel.gg.presentation.viewmodel.EditarGastoViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditarGastoScreen(
    gastoId  : String,
    onVolver : () -> Unit,
    viewModel: EditarGastoViewModel = koinInject()
) {
    val state    by viewModel.state.collectAsState()
    val snackbar  = remember { SnackbarHostState() }
    var dropdownExpanded by remember { mutableStateOf(false) }
    var mostrarDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.cargar(gastoId) }

    LaunchedEffect(state.guardadoExitoso) {
        if (state.guardadoExitoso) {
            snackbar.showSnackbar(Strings.t("editar.actualizado"))
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
                title = { Text(if (state.esCuota) tr("editar.tituloCuota") else tr("editar.tituloGasto")) },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(AppIcons.Volver, contentDescription = tr("comun.volver"))
                    }
                }
            )
        }
    ) { padding ->

        if (state.cargando) return@Scaffold

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value         = state.monto,
                onValueChange = viewModel::onMontoChange,
                label         = { Text(tr("registrar.monto")) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier      = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value         = state.descripcion,
                onValueChange = viewModel::onDescripcionChange,
                label         = { Text(tr("registrar.descripcion")) },
                modifier      = Modifier.fillMaxWidth()
            )

            ExposedDropdownMenuBox(
                expanded         = dropdownExpanded,
                onExpandedChange = { dropdownExpanded = it }
            ) {
                OutlinedTextField(
                    value         = state.categorias
                        .find { it.id == state.categoriaId }?.nombre ?: "",
                    onValueChange = {},
                    readOnly      = true,
                    label         = { Text(tr("registrar.categoria")) },
                    trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(dropdownExpanded) },
                    modifier      = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded         = dropdownExpanded,
                    onDismissRequest = { dropdownExpanded = false }
                ) {
                    state.categorias.forEach { cat ->
                        DropdownMenuItem(
                            text    = { Text(cat.nombre) },
                            onClick = {
                                viewModel.onCategoriaChange(cat.id)
                                dropdownExpanded = false
                            }
                        )
                    }
                }
            }

            // Fecha con selector de calendario
            Surface(
                onClick   = { mostrarDatePicker = true },
                shape     = RoundedCornerShape(4.dp),
                color     = AppTheme.CardDark,
                border    = BorderStroke(1.dp, AppTheme.BorderColor),
                modifier  = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier              = Modifier.padding(horizontal = 16.dp, vertical = 12.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Column {
                        Text(tr("registrar.fecha"), color = AppTheme.TextMuted, fontSize = 12.sp)
                        Text(
                            state.fecha.ifBlank { tr("registrar.seleccionarFecha") },
                            color = if (state.fecha.isBlank()) AppTheme.TextSubtle else AppTheme.TextPrimary
                        )
                    }
                    Icon(AppIcons.Calendario, contentDescription = tr("registrar.seleccionarFecha"), tint = AppTheme.TextMuted)
                }
            }

            if (mostrarDatePicker) {
                val millisInicial = try {
                    java.time.LocalDate.parse(state.fecha)
                        .atStartOfDay(java.time.ZoneOffset.UTC)
                        .toInstant()
                        .toEpochMilli()
                } catch (_: Exception) { null }

                val datePickerState = rememberDatePickerState(
                    initialSelectedDateMillis = millisInicial
                )

                DatePickerDialog(
                    onDismissRequest = { mostrarDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            datePickerState.selectedDateMillis?.let { millis ->
                                val fecha = java.time.Instant.ofEpochMilli(millis)
                                    .atZone(java.time.ZoneOffset.UTC)
                                    .toLocalDate()
                                viewModel.onFechaChange(fecha.toString())
                            }
                            mostrarDatePicker = false
                        }) { Text("OK", color = AppTheme.Indigo) }
                    },
                    dismissButton = {
                        TextButton(onClick = { mostrarDatePicker = false }) { Text(tr("comun.cancelar"), color = AppTheme.TextMuted) }
                    },
                ) {
                    DatePicker(state = datePickerState)
                }
            }

            // Si es cuota, mostrar el selector de alcance
            if (state.esCuota) {
                Text(
                    tr("editar.aplicarCambio"),
                    style = MaterialTheme.typography.labelMedium
                )
                AlcanceSelector(
                    seleccionado = state.alcanceEdicion,
                    onSeleccionar = viewModel::onAlcanceChange
                )
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick  = viewModel::guardar,
                enabled  = !state.cargando,
                modifier = Modifier.fillMaxWidth()
            ) { Text(tr("editar.guardarCambios")) }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun AlcanceSelector(
    seleccionado : String,
    onSeleccionar: (String) -> Unit
) {
    listOf(
        "SOLO_ESTA"     to { Strings.t("editar.soloEsta") },
        "FUTURAS"       to { Strings.t("editar.estaYLasSiguientes") },
        "TODA_LA_SERIE" to { Strings.t("editar.todaLaSerie") }
    ).forEach { (valor, label) ->
        androidx.compose.foundation.layout.Row(
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            modifier          = Modifier.fillMaxWidth()
        ) {
            androidx.compose.material3.RadioButton(
                selected = seleccionado == valor,
                onClick  = { onSeleccionar(valor) }
            )
            Text(label())
        }
    }
}