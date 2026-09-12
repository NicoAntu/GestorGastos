package com.angel.gg.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.Icons
import com.angel.gg.presentation.screen.AppIcons
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.BlendMode.Companion.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.angel.gg.presentation.AppTheme
import com.angel.gg.presentation.i18n.tr
import com.angel.gg.presentation.viewmodel.RegistrarGastoViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrarGastoScreen(
    anio             : Int,
    mes              : Int,
    onVolver         : () -> Unit,
    onGuardadoExitoso: () -> Unit = {},
    viewModel        : RegistrarGastoViewModel = koinInject()
) {
    val state    by viewModel.state.collectAsState()
    val snackbar  = remember { SnackbarHostState() }
    var dropdownExpanded by remember { mutableStateOf(false) }
    var mostrarDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(state.guardadoExitoso) {
        if (state.guardadoExitoso) {
            viewModel.resetearFormulario()
            onGuardadoExitoso()
        }
    }

    LaunchedEffect(Unit) { viewModel.inicializarFecha(anio, mes) }

    LaunchedEffect(state.mensajeError) {
        state.mensajeError?.let {
            snackbar.showSnackbar(it)
            viewModel.limpiarMensajes()
        }
    }

    Scaffold(
        snackbarHost   = { SnackbarHost(snackbar) },
        containerColor = AppTheme.BgDark,
        topBar = {
            TopAppBar(
                title  = {
                    Text(
                        if (state.esCuota) tr("registrar.tituloCuotas") else tr("registrar.tituloGasto"),
                        color = AppTheme.TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(AppIcons.Volver, contentDescription = tr("comun.volver"), tint = AppTheme.TextMuted)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppTheme.CardDark)
            )
        }
    ) { padding ->

        // Si no hay categorías, mostrar mensaje en lugar de pantalla vacía
        if (state.categorias.isEmpty()) {
            Box(
                modifier         = Modifier.fillMaxSize().padding(padding).background(AppTheme.BgDark),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier            = Modifier.padding(32.dp)
                ) {
                    Text(tr("registrar.sinCategorias"), color = AppTheme.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(
                        tr("registrar.sinCategoriasDesc"),
                        color     = AppTheme.TextMuted,
                        fontSize  = 14.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = onVolver,
                        colors  = ButtonDefaults.buttonColors(containerColor = AppTheme.Indigo)
                    ) { Text(tr("comun.volver")) }
                }
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
                .background(AppTheme.BgDark),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            // Campo monto
            OutlinedTextField(
                value           = state.monto,
                onValueChange   = viewModel::onMontoChange,
                label           = { Text(tr("registrar.monto"), color = AppTheme.TextMuted) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors          = outlinedFieldColors(),
                modifier        = Modifier.fillMaxWidth()
            )

            // Campo descripción
            OutlinedTextField(
                value         = state.descripcion,
                onValueChange = viewModel::onDescripcionChange,
                label         = { Text(tr("registrar.descripcion"), color = AppTheme.TextMuted) },
                colors        = outlinedFieldColors(),
                modifier      = Modifier.fillMaxWidth()
            )

            // Selector de categoría
            ExposedDropdownMenuBox(
                expanded         = dropdownExpanded,
                onExpandedChange = { dropdownExpanded = it }
            ) {
                OutlinedTextField(
                    value         = state.categorias.find { it.id == state.categoriaId }?.nombre ?: "",
                    onValueChange = {},
                    readOnly      = true,
                    label         = { Text(tr("registrar.categoria"), color = AppTheme.TextMuted) },
                    trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(dropdownExpanded) },
                    colors        = outlinedFieldColors(),
                    modifier      = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded         = dropdownExpanded,
                    onDismissRequest = { dropdownExpanded = false },
                    containerColor   = AppTheme.CardDark
                ) {
                    state.categorias.forEach { cat ->
                        DropdownMenuItem(
                            text    = { Text(cat.nombre, color = AppTheme.TextPrimary) },
                            onClick = { viewModel.onCategoriaChange(cat.id); dropdownExpanded = false }
                        )
                    }
                }
            }

            // Fecha con selector de calendario
            Surface(
                onClick   = { mostrarDatePicker = true },
                shape     = RoundedCornerShape(4.dp),
                color     = AppTheme.CardDark,
                border    = androidx.compose.foundation.BorderStroke(1.dp, AppTheme.BorderColor),
                modifier  = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier            = Modifier.padding(horizontal = 16.dp, vertical = 12.dp).fillMaxWidth(),
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
                                viewModel.onFechaSeleccionada(fecha.toString())
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

            // Toggle cuotas
            Card(
                colors = CardDefaults.cardColors(containerColor = AppTheme.CardDark),
                shape  = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier              = Modifier.padding(16.dp).fillMaxWidth(),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(tr("registrar.pagoEnCuotas"), color = AppTheme.TextPrimary, fontWeight = FontWeight.Medium)
                        Text(tr("registrar.divideMonto"), color = AppTheme.TextMuted, fontSize = 12.sp)
                    }
                    Switch(
                        checked         = state.esCuota,
                        onCheckedChange = viewModel::onToggleCuota,
                        colors          = SwitchDefaults.colors(checkedThumbColor = AppTheme.White, checkedTrackColor = AppTheme.Indigo)
                    )
                }
            }

            // Sección cuotas
            if (state.esCuota) {
                OutlinedTextField(
                    value           = state.cantCuotas,
                    onValueChange   = viewModel::onCantCuotasChange,
                    label           = { Text(tr("registrar.cantCuotas"), color = AppTheme.TextMuted) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors          = outlinedFieldColors(),
                    modifier        = Modifier.fillMaxWidth()
                )

                // Preview cuotas
                val cant  = state.cantCuotas.toIntOrNull() ?: 0
                val monto = state.monto.toDoubleOrNull()
                if (cant >= 2 && monto != null && monto > 0) {
                    Card(
                        colors   = CardDefaults.cardColors(containerColor = AppTheme.Indigo.copy(alpha = 0.1f)),
                        shape    = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(tr("registrar.vistaPrevia"), color = AppTheme.IndigoLight, fontSize = 12.sp)
                            Text(
                                tr("registrar.esteMesCuota", cant),
                                color      = AppTheme.TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize   = 15.sp
                            )
                            Text(
                                tr("registrar.porCuotaTotal", formatMonto(monto / cant), formatMonto(monto)),
                                color    = AppTheme.TextMuted,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick  = viewModel::guardar,
                enabled  = !state.cargando,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = AppTheme.Indigo),
                shape    = RoundedCornerShape(12.dp)
            ) {
                Text(
                    if (state.esCuota) tr("registrar.generarCuotas", state.cantCuotas) else tr("registrar.guardarGasto"),
                    fontWeight = FontWeight.Bold,
                    fontSize   = 15.sp
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}