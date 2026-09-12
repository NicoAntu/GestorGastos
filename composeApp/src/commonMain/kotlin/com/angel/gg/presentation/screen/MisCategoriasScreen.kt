package com.angel.gg.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.angel.gg.domain.model.Categoria
import com.angel.gg.presentation.AppTheme
import com.angel.gg.presentation.i18n.tr
import com.angel.gg.presentation.viewmodel.CategoriaViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MisCategoriasScreen(
    onVolver : () -> Unit,
    viewModel: CategoriaViewModel = koinInject()
) {
    val state   by viewModel.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    var mostrarDialogo     by remember { mutableStateOf(false) }
    var mostrarColorPicker by remember { mutableStateOf(false) }
    var categoriaEditando  by remember { mutableStateOf<Categoria?>(null) }
    var nombre             by remember { mutableStateOf("") }
    var descripcion        by remember { mutableStateOf("") }
    var color              by remember { mutableStateOf("#4F46E5") }
    var fijada             by remember { mutableStateOf(false) }

    LaunchedEffect(state.mensajeExito, state.mensajeError) {
        state.mensajeExito?.let { snackbar.showSnackbar(it); viewModel.limpiarMensajes() }
        state.mensajeError?.let { snackbar.showSnackbar(it); viewModel.limpiarMensajes() }
    }

    if (mostrarColorPicker) {
        ColorPickerDialog(
            colorInicial    = color,
            onColorSelected = { color = it },
            onDismiss       = { mostrarColorPicker = false }
        )
    }

    Scaffold(
        containerColor = AppTheme.BgDark,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(tr("ajustes.misCategorias"), color = AppTheme.TextPrimary, fontWeight = FontWeight.Bold)
                        Text(tr("micat.categorias", state.categorias.size), color = AppTheme.TextMuted, fontSize = 12.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(AppIcons.Volver, contentDescription = tr("comun.volver"), tint = AppTheme.TextMuted)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        categoriaEditando = null
                        nombre = ""; descripcion = ""; color = "#4F46E5"; fijada = false
                        mostrarDialogo = true
                    }) {
                        Icon(AppIcons.Agregar, contentDescription = tr("micat.nueva"), tint = AppTheme.TextMuted)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppTheme.CardDark)
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        LazyColumn(
            modifier            = Modifier.fillMaxSize().padding(padding).background(AppTheme.BgDark),
            contentPadding      = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(state.categorias, key = { it.id }) { cat ->
                val catColor = parsearColor(cat.color) ?: AppTheme.Indigo
                Card(
                    colors   = CardDefaults.cardColors(containerColor = catColor.copy(alpha = 0.12f)),
                    shape    = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier          = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            cat.nombre.take(1).uppercase(),
                            color      = catColor,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize   = 28.sp,
                            textAlign  = TextAlign.Center,
                            modifier   = Modifier.width(36.dp)
                        )
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(cat.nombre, color = AppTheme.TextPrimary, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                            cat.descripcion?.let { Text(it, color = AppTheme.TextMuted, fontSize = 12.sp) }
                        }
                        Row {
                            IconButton(
                                onClick  = {
                                    categoriaEditando = cat
                                    nombre = cat.nombre
                                    descripcion = cat.descripcion ?: ""
                                    color = cat.color
                                    fijada = cat.fijada
                                    mostrarDialogo = true
                                },
                                modifier = Modifier.size(36.dp).clip(RoundedCornerShape(100.dp)).background(AppTheme.CardMedium)
                            ) {
                                Icon(AppIcons.Editar, contentDescription = tr("micat.editarAccion"), tint = AppTheme.TextMuted, modifier = Modifier.size(18.dp))
                            }
                            Spacer(Modifier.width(18.dp))
                            IconButton(
                                onClick  = { viewModel.eliminar(cat.id) },
                                modifier = Modifier.size(36.dp).clip(RoundedCornerShape(100.dp)).background(AppTheme.Red.copy(alpha = 0.1f))
                            ) {
                                Icon(AppIcons.Eliminar, contentDescription = tr("comun.eliminar"), tint = AppTheme.Red, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    if (mostrarDialogo) {
        AlertDialog(
            onDismissRequest = { mostrarDialogo = false },
            containerColor   = AppTheme.CardDark,
            title = { Text(if (categoriaEditando == null) tr("micat.nueva") else tr("micat.editar"), color = AppTheme.TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = nombre,
                        onValueChange = { nombre = it },
                        label = { Text(tr("micat.nombre"), color = AppTheme.TextMuted) },
                        colors = outlinedFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = descripcion,
                        onValueChange = { descripcion = it },
                        label = { Text(tr("micat.descripcionOpcional"), color = AppTheme.TextMuted) },
                        colors = outlinedFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(AppTheme.CardMedium)
                            .clickable { mostrarColorPicker = true }
                            .padding(16.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(parsearColor(color) ?: AppTheme.Indigo))
                        Column {
                            Text(tr("micat.color"), color = AppTheme.TextMuted, fontSize = 11.sp)
                            Text(color, color = AppTheme.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }
                        Spacer(Modifier.weight(1f))
                        Text(tr("micat.cambiar"), color = AppTheme.Indigo, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cat = categoriaEditando
                        if (cat == null) viewModel.crear(nombre, descripcion.ifBlank { null }, color, "default", fijada)
                        else viewModel.editar(cat.id, nombre, descripcion.ifBlank { null }, color, cat.icono, fijada)
                        mostrarDialogo = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AppTheme.Indigo)
                ) { Text(tr("comun.guardar")) }
            },
            dismissButton = { TextButton(onClick = { mostrarDialogo = false }) { Text(tr("comun.cancelar"), color = AppTheme.TextMuted) } }
        )
    }
}