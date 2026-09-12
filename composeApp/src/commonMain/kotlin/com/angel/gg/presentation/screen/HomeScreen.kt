package com.angel.gg.presentation.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.angel.gg.domain.model.Categoria
import com.angel.gg.domain.model.Gasto
import com.angel.gg.presentation.AppTheme
import com.angel.gg.presentation.i18n.Money
import com.angel.gg.presentation.i18n.Strings
import com.angel.gg.presentation.i18n.tr
import com.angel.gg.presentation.viewmodel.CategoriaViewModel
import com.angel.gg.presentation.viewmodel.HomeUiState
import com.angel.gg.presentation.viewmodel.HomeViewModel
import org.koin.compose.koinInject

private val CATEGORY_COLORS = listOf(
    AppTheme.Blue, AppTheme.Green, AppTheme.Yellow,
    AppTheme.Purple, AppTheme.Orange, AppTheme.Red, AppTheme.Indigo
)

@Composable
fun HomeScreen(
    onNavegarRegistrar        : () -> Unit,
    onNavegarBuscador         : () -> Unit,
    onNavegarEstadisticas     : () -> Unit,
    onNavegarCategoriaDetalle : () -> Unit,
    onNavegarAjustes          : () -> Unit,
    onVerDetalleCuotas        : (String) -> Unit,
    onEditarGasto             : (String) -> Unit,
    homeViewModel             : HomeViewModel      = koinInject(),
    catViewModel              : CategoriaViewModel = koinInject()
) {
    val homeState  by homeViewModel.state.collectAsState()
    val catState   by catViewModel.state.collectAsState()
    val tabActivo  by homeViewModel.tabActivo.collectAsState()
    val snackbar    = remember { SnackbarHostState() }

    var mostrarDialogoIngreso by remember { mutableStateOf(false) }
    var montoIngreso          by remember { mutableStateOf("") }
    var gastoAEliminar        by remember { mutableStateOf<Gasto?>(null) }

    LaunchedEffect(homeState.mensajeExito, homeState.mensajeError) {
        homeState.mensajeExito?.let { snackbar.showSnackbar(it); homeViewModel.limpiarMensajes() }
        homeState.mensajeError?.let { snackbar.showSnackbar(it); homeViewModel.limpiarMensajes() }
    }

    Scaffold(
        snackbarHost   = { SnackbarHost(snackbar) },
        containerColor = AppTheme.BgDark,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppTheme.CardDark)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                // Navegación mensual
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier          = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(AppTheme.CardMedium)
                        .padding(horizontal = 4.dp, vertical = 4.dp)
                ) {
                    IconButton(onClick = homeViewModel::retrocederMes, modifier = Modifier.size(32.dp)) {
                        Text("‹", color = AppTheme.TextMuted, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        "${Strings.mes(homeState.mesActual)} ${homeState.anioActual}",
                        color      = AppTheme.TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize   = 15.sp,
                        modifier   = Modifier.padding(horizontal = 4.dp)
                    )
                    IconButton(onClick = homeViewModel::avanzarMes, modifier = Modifier.size(32.dp)) {
                        Text("›", color = AppTheme.TextMuted, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Acciones
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(onClick = onNavegarBuscador) {
                        Icon(AppIcons.Buscar, contentDescription = tr("home.buscar"), tint = AppTheme.TextMuted)
                    }
                    Button(
                        onClick        = onNavegarRegistrar,
                        colors         = ButtonDefaults.buttonColors(containerColor = AppTheme.Indigo),
                        shape          = RoundedCornerShape(50),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Icon(AppIcons.Agregar, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(tr("home.nuevoGasto"), fontSize = 13.sp)
                    }
                    IconButton(onClick = onNavegarAjustes) {
                        Icon(AppIcons.Ajustes, contentDescription = tr("ajustes.titulo"), tint = AppTheme.TextMuted)
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = AppTheme.CardDark,
                contentColor   = AppTheme.TextMuted,
                modifier       = Modifier.navigationBarsPadding()
            ) {
                listOf(
                    Triple(0, "nav.dashboard", "◎"),
                    Triple(1, "nav.gastos",    "≡"),
                    Triple(2, "nav.estadisticas", "◈")
                ).forEach { (idx, label, icono) ->
                    NavigationBarItem(
                        selected = tabActivo == idx,
                        onClick  = { if (idx == 2) onNavegarEstadisticas() else homeViewModel.setTab(idx) },
                        icon     = {
                            Text(icono, fontSize = 18.sp,
                                color = if (tabActivo == idx) AppTheme.Indigo else AppTheme.TextSubtle)
                        },
                        label   = {
                            Text(tr(label), fontSize = 11.sp,
                                color = if (tabActivo == idx) AppTheme.Indigo else AppTheme.TextSubtle)
                        },
                        colors  = NavigationBarItemDefaults.colors(
                            selectedIconColor   = AppTheme.Indigo,
                            selectedTextColor   = AppTheme.Indigo,
                            unselectedIconColor = AppTheme.TextSubtle,
                            unselectedTextColor = AppTheme.TextSubtle,
                            indicatorColor      = Color.Transparent
                        )
                    )
                }
            }
        }
    ) { padding ->
        AnimatedContent(
            targetState    = tabActivo,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            modifier       = Modifier.padding(padding)
        ) { tab ->
            when (tab) {
                0 -> DashboardTab(
                    homeState           = homeState,
                    categorias          = catState.categorias,
                    onVerDetalleCuotas  = onVerDetalleCuotas,
                    onEditarIngreso     = { mostrarDialogoIngreso = true },
                    onVerTodosGastos    = { homeViewModel.setTab(1) },
                    onVerTodasCats      = onNavegarCategoriaDetalle,
                    onVerDetallesCuotas = onNavegarEstadisticas
                )
                1 -> GastosTab(
                    gastos       = homeState.gastos,
                    mes          = homeState.mesActual,
                    anio         = homeState.anioActual,
                    onEditar     = onEditarGasto,
                    onVerDetalle = onVerDetalleCuotas,
                    onEliminar   = { gastoAEliminar = it }
                )
            }
        }
    }

    // Diálogo ingreso mensual
    if (mostrarDialogoIngreso) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoIngreso = false },
            containerColor   = AppTheme.CardDark,
            title = { Text(tr("home.ingresoDelMes"), color = AppTheme.TextPrimary, fontWeight = FontWeight.Bold) },
            text  = {
                OutlinedTextField(
                    value         = montoIngreso,
                    onValueChange = { montoIngreso = it },
                    label         = { Text(tr("home.monto"), color = AppTheme.TextMuted) },
                    placeholder   = { Text(tr("home.ejMonto"), color = AppTheme.TextSubtle) },
                    colors        = outlinedFieldColors()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val monto = montoIngreso.toDoubleOrNull() ?: 0.0
                        homeViewModel.guardarIngreso(monto)
                        mostrarDialogoIngreso = false
                        montoIngreso = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AppTheme.Indigo)
                ) { Text(tr("comun.guardar")) }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoIngreso = false }) {
                    Text(tr("comun.cancelar"), color = AppTheme.TextMuted)
                }
            }
        )
    }

    // Diálogo confirmar eliminación
    gastoAEliminar?.let { gasto ->
        AlertDialog(
            onDismissRequest = { gastoAEliminar = null },
            containerColor   = AppTheme.CardDark,
            title = { Text(tr("home.eliminarGasto"), color = AppTheme.TextPrimary, fontWeight = FontWeight.Bold) },
            text  = { Text(tr("home.confirmarEliminar", gasto.descripcion), color = AppTheme.TextMuted) },
            confirmButton = {
                Button(
                    onClick = { homeViewModel.eliminarGasto(gasto); gastoAEliminar = null },
                    colors  = ButtonDefaults.buttonColors(containerColor = AppTheme.Red)
                ) { Text(tr("comun.eliminar")) }
            },
            dismissButton = {
                TextButton(onClick = { gastoAEliminar = null }) {
                    Text(tr("comun.cancelar"), color = AppTheme.TextMuted)
                }
            }
        )
    }
}

// ── Tab Dashboard ─────────────────────────────────────────────────────────────

@Composable
private fun DashboardTab(
    homeState           : HomeUiState,
    categorias          : List<Categoria>,
    onVerDetalleCuotas  : (String) -> Unit,
    onEditarIngreso     : () -> Unit,
    onVerTodosGastos    : () -> Unit,
    onVerTodasCats      : () -> Unit,
    onVerDetallesCuotas : () -> Unit
) {
    val ingreso      = homeState.ingresoMensual?.monto ?: 0.0
    val totalGastado = homeState.totalGastado
    val porcentaje   = if (ingreso > 0) (totalGastado / ingreso).coerceIn(0.0, 1.0) else 0.0
    val balance      = ingreso - totalGastado
    val totalCuotas  = homeState.gastos.filter { it.esCuota }.sumOf { it.monto }
    val cantCuotas   = homeState.gastos.count { it.esCuota }

    LazyColumn(
        modifier            = Modifier.fillMaxSize().background(AppTheme.BgDark),
        contentPadding      = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            StatCard(
                modifier        = Modifier.fillMaxWidth(),
                label           = tr("home.balanceDisponible"),
                value           = formatMontoConSigno(balance),
                valueColor      = if (balance < 0) AppTheme.Red else AppTheme.TextPrimary,
                subLabel        = tr("home.de", formatMonto(ingreso)),
                progress        = porcentaje.toFloat(),
                progressColor   = if (porcentaje > 0.8) AppTheme.Red else AppTheme.Yellow,
                porcentajeLabel = tr("home.porcentajeConsumido", "%.1f".format(porcentaje * 100)),
                onEdit          = onEditarIngreso
            )
        }

        // Misma altura con IntrinsicSize.Max
        item {
            Row(
                modifier              = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MiniStatCard(
                    modifier   = Modifier.weight(1f).fillMaxHeight(),
                    label      = tr("home.gastoTotal"),
                    value      = formatMonto(totalGastado),
                    subLabel   = tr("home.enMovimientos", homeState.gastos.size),
                    valueColor = AppTheme.Red
                )
                MiniStatCard(
                    modifier   = Modifier.weight(1f).fillMaxHeight(),
                    label      = tr("home.gastosDeCuotas"),
                    value      = formatMonto(totalCuotas),
                    subLabel   = tr("home.cuotasActivas", cantCuotas),
                    extraLabel = tr("home.verDetalles"),
                    bgGradient = true,
                    onClick    = onVerDetallesCuotas
                )
            }
        }

        if (homeState.gastos.isNotEmpty()) {
            item {
                CategoriaBreakdown(
                    gastos     = homeState.gastos,
                    categorias = categorias,
                    total      = totalGastado,
                    onVerTodas = onVerTodasCats
                )
            }
        }

        item {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(tr("home.ultimosMovimientos"), color = AppTheme.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(
                    tr("home.verTodos"),
                    color    = AppTheme.Indigo,
                    fontSize = 13.sp,
                    modifier = Modifier.clickable { onVerTodosGastos() }
                )
            }
        }

        items(homeState.gastos.take(5), key = { it.id }) { gasto ->
            GastoItemDark(
                gasto       = gasto,
                categorias  = categorias,
                onEditar    = {},
                onEliminar  = {},
                onClick     = { if (gasto.esCuota) onVerDetalleCuotas(gasto.id) },
                showActions = false
            )
        }

        item { Spacer(Modifier.height(8.dp)) }
    }
}

// ── Tarjetas ──────────────────────────────────────────────────────────────────

@Composable
private fun StatCard(
    modifier        : Modifier,
    label           : String,
    value           : String,
    valueColor      : Color  = AppTheme.TextPrimary,
    subLabel        : String,
    progress        : Float,
    progressColor   : Color,
    porcentajeLabel : String,
    onEdit          : () -> Unit
) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = AppTheme.CardDark), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(label, color = AppTheme.TextMuted, fontSize = 13.sp)
                IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                    Icon(AppIcons.Editar, contentDescription = tr("home.editarIngreso"), tint = AppTheme.TextSubtle, modifier = Modifier.size(22.dp))
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(value, color = valueColor, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text(subLabel, color = AppTheme.TextMuted, fontSize = 13.sp)
            Spacer(Modifier.height(16.dp))
            LinearProgressIndicator(
                progress   = { progress },
                modifier   = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)),
                color      = progressColor,
                trackColor = AppTheme.CardMedium
            )
            Spacer(Modifier.height(4.dp))
            Text(porcentajeLabel, color = AppTheme.TextMuted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun MiniStatCard(
    modifier   : Modifier,
    label      : String,
    value      : String,
    subLabel   : String,
    extraLabel : String?       = null,
    valueColor : Color         = AppTheme.TextPrimary,
    bgGradient : Boolean       = false,
    onClick    : (() -> Unit)? = null
) {
    Card(
        modifier = if (onClick != null) modifier.clickable { onClick() } else modifier,
        colors   = CardDefaults.cardColors(containerColor = if (bgGradient) AppTheme.Indigo.copy(alpha = 0.15f) else AppTheme.CardDark),
        shape    = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(label, color = AppTheme.TextMuted, fontSize = 12.sp)
            Text(
                value,
                color      = if (bgGradient) AppTheme.IndigoLight else valueColor,
                fontSize   = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                subLabel,
                color    = if (bgGradient) AppTheme.IndigoLight.copy(alpha = 0.7f) else AppTheme.TextMuted,
                fontSize = 11.sp
            )
            extraLabel?.let { Text(it, color = AppTheme.IndigoLight, fontSize = 11.sp, fontWeight = FontWeight.Medium) }
        }
    }
}

@Composable
private fun CategoriaBreakdown(
    gastos    : List<Gasto>,
    categorias: List<Categoria>,
    total     : Double,
    onVerTodas: () -> Unit
) {
    val gastosPorCat = gastos.groupBy { it.categoriaId }
        .mapValues { (_, gs) -> gs.sumOf { it.monto } }
        .entries.sortedByDescending { it.value }.take(5)

    Card(colors = CardDefaults.cardColors(containerColor = AppTheme.CardDark), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(tr("home.gastosPorCategoria"), color = AppTheme.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(tr("home.verTodas"), color = AppTheme.Indigo, fontSize = 12.sp, modifier = Modifier.clickable { onVerTodas() })
            }
            Spacer(Modifier.height(16.dp))
            gastosPorCat.forEachIndexed { idx, (catId, monto) ->
                val cat    = categorias.find { it.id == catId }
                val nombre = cat?.nombre ?: gastos.find { it.categoriaId == catId }?.categoriaNombre ?: tr("home.sinCategoria")
                val color  = parsearColor(cat?.color) ?: CATEGORY_COLORS[idx % CATEGORY_COLORS.size]
                val pct    = if (total > 0) (monto / total).toFloat() else 0f
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(color.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                                Text(nombre.take(1).uppercase(), color = color, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Spacer(Modifier.width(10.dp))
                            Text(nombre, color = AppTheme.TextPrimary, fontSize = 14.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(formatMonto(monto), color = AppTheme.TextPrimary, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                            Text("(${"%.0f".format(pct * 100)}%)", color = AppTheme.TextMuted, fontSize = 12.sp)
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(progress = { pct }, modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(50)), color = color, trackColor = AppTheme.CardMedium)
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }
}

// ── Tab Gastos ────────────────────────────────────────────────────────────────

@Composable
private fun GastosTab(
    gastos      : List<Gasto>,
    mes         : Int,
    anio        : Int,
    onEditar    : (String) -> Unit,
    onVerDetalle: (String) -> Unit,
    onEliminar  : (Gasto) -> Unit,
    catViewModel: CategoriaViewModel = koinInject()
) {
    val catState by catViewModel.state.collectAsState()

    LazyColumn(
        modifier            = Modifier.fillMaxSize().background(AppTheme.BgDark),
        contentPadding      = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = AppTheme.CardDark), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(tr("home.todosLosMovimientos", Strings.mes(mes), anio), color = AppTheme.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Spacer(Modifier.height(2.dp))
                        Text(tr("home.registros", gastos.size), color = AppTheme.TextMuted, fontSize = 13.sp)
                    }
                }
            }
        }
        if (gastos.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
                    Text(tr("home.sinMovimientos"), color = AppTheme.TextSubtle, fontSize = 15.sp)
                }
            }
        } else {
            items(gastos, key = { it.id }) { gasto ->
                GastoItemDark(
                    gasto      = gasto,
                    categorias = catState.categorias,
                    onEditar   = { onEditar(gasto.id) },
                    onEliminar = { onEliminar(gasto) },
                    onClick    = { if (gasto.esCuota) onVerDetalle(gasto.id) }
                )
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

// ── GastoItem dark ────────────────────────────────────────────────────────────

@Composable
internal fun GastoItemDark(
    gasto      : Gasto,
    categorias : List<Categoria>,
    onEditar   : () -> Unit,
    onEliminar : () -> Unit,
    onClick    : () -> Unit,
    showActions: Boolean = true
) {
    val catColor = parsearColor(categorias.find { it.id == gasto.categoriaId }?.color) ?: AppTheme.Blue

    Card(onClick = onClick, colors = CardDefaults.cardColors(containerColor = catColor.copy(alpha = 0.12f)),
        shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(14.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(42.dp).clip(RoundedCornerShape(10.dp)).background(catColor.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
                Text(gasto.categoriaNombre.take(1).uppercase(), color = catColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(gasto.descripcion, color = AppTheme.TextPrimary, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(gasto.categoriaNombre, color = AppTheme.TextMuted, fontSize = 12.sp)
                    Text("•", color = AppTheme.TextSubtle, fontSize = 12.sp)
                    val fechaDisplay = try {
                        val p = gasto.fecha.split("-")
                        "${p[2]} ${Strings.mes(gasto.mes).take(3)}"
                    } catch (e: Exception) { gasto.fecha }
                    Text(fechaDisplay, color = AppTheme.TextMuted, fontSize = 12.sp)
                    if (gasto.esCuota && gasto.cuotaActual != null && gasto.cuotaTotal != null) {
                        Spacer(Modifier.width(2.dp))
                        Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(AppTheme.Indigo.copy(alpha = 0.2f)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                            Text("${gasto.cuotaActual}/${gasto.cuotaTotal}", color = AppTheme.IndigoLight, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            Text("-${formatMonto(gasto.monto)}", color = AppTheme.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            if (showActions) {
                Spacer(Modifier.width(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    IconButton(onClick = onEditar, modifier = Modifier.size(24.dp)) {
                        Icon(AppIcons.Editar, null, tint = AppTheme.TextSubtle, modifier = Modifier.size(12.dp))
                    }
                    IconButton(onClick = onEliminar, modifier = Modifier.size(24.dp)) {
                        Icon(AppIcons.Eliminar, null, tint = AppTheme.Red.copy(alpha = 0.7f), modifier = Modifier.size(12.dp))
                    }
                }
            }
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

@Composable
internal fun formatMonto(monto: Double): String = Money.format(monto)

@Composable
internal fun formatMontoConSigno(monto: Double): String = Money.formatConSigno(monto)

internal fun parsearColor(hex: String?): Color? {
    if (hex == null) return null
    return try {
        val h = hex.removePrefix("#")
        Color(red = h.substring(0,2).toInt(16)/255f, green = h.substring(2,4).toInt(16)/255f, blue = h.substring(4,6).toInt(16)/255f)
    } catch (e: Exception) { null }
}

@Composable
internal fun outlinedFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor      = AppTheme.Indigo,
    unfocusedBorderColor    = AppTheme.BorderColor,
    focusedTextColor        = AppTheme.TextPrimary,
    unfocusedTextColor      = AppTheme.TextPrimary,
    cursorColor             = AppTheme.Indigo,
    focusedLabelColor       = AppTheme.Indigo,
    unfocusedLabelColor     = AppTheme.TextMuted,
    focusedContainerColor   = AppTheme.CardMedium.copy(alpha = 0.3f),
    unfocusedContainerColor = Color.Transparent
)