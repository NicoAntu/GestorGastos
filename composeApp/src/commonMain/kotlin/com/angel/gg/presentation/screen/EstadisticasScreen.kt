package com.angel.gg.presentation.screen

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.angel.gg.presentation.AppTheme
import com.angel.gg.presentation.i18n.Money
import com.angel.gg.presentation.i18n.tr
import com.angel.gg.presentation.viewmodel.EstadisticasViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EstadisticasScreen(
    onVolver         : () -> Unit,
    onVerDetalleCuota: (String) -> Unit,
    onVerCategorias  : () -> Unit = {},    // nuevo
    onVerTodasCuotas : () -> Unit = {},    // nuevo
    viewModel        : EstadisticasViewModel = koinInject()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        containerColor = AppTheme.BgDark,
        topBar = {
            TopAppBar(
                title = { Text(tr("nav.estadisticas"), color = AppTheme.TextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(AppIcons.Volver, contentDescription = tr("comun.volver"), tint = AppTheme.TextMuted)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppTheme.CardDark)
            )
        }
    ) { padding ->

        LazyColumn(
            modifier            = Modifier.fillMaxSize().padding(padding).background(AppTheme.BgDark),
            contentPadding      = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ── Fila: Gauge + Promedio ────────────────────────────────
            item {
                BoxWithConstraints {
                    if (maxWidth < 500.dp) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            GaugeBalanceCard(state, Modifier.fillMaxWidth())
                            PromedioMensualCard(state, Modifier.fillMaxWidth(), fontSize = 32.sp)
                        }
                    } else {
                        Row(
                            modifier            = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            GaugeBalanceCard(state, Modifier.weight(1f).fillMaxHeight())
                            PromedioMensualCard(state, Modifier.weight(1f).fillMaxHeight(), fontSize = 38.sp)
                        }
                    }
                }
            }

            // ── Gastos por Categorías (Pie Chart) ────────────────────
            if (state.gastosPorCategoria.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors   = CardDefaults.cardColors(containerColor = AppTheme.CardDark),
                        shape    = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(
                                modifier              = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(tr("est.gastosPorCategorias"), color = AppTheme.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text(
                                    tr("home.verTodas"),
                                    color    = AppTheme.Indigo,
                                    fontSize = 13.sp,
                                    modifier = Modifier.clickable { onVerCategorias() }
                                )
                            }
                            Spacer(Modifier.height(20.dp))

                            Box(
                                modifier            = Modifier.fillMaxWidth(),
                                contentAlignment    = Alignment.Center
                            ) {
                                val total = state.gastosPorCategoria.sumOf { it.second }
                                PieChartPainter(
                                    datos    = state.gastosPorCategoria,
                                    total    = total,
                                    modifier = Modifier.size(180.dp)
                                )
                            }

                            Spacer(Modifier.height(16.dp))

                            // Leyenda
                            val coloresCat = listOf(
                                AppTheme.Blue, AppTheme.Yellow, AppTheme.Green,
                                AppTheme.Purple, AppTheme.Orange, AppTheme.Red
                            )
                            FlowRow(
                                modifier          = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                                verticalArrangement   = Arrangement.spacedBy(4.dp)
                            ) {
                                state.gastosPorCategoria.forEachIndexed { idx, (nombre, _, colorHex) ->
                                    val color = parsearColorStats(colorHex) ?: coloresCat[idx % coloresCat.size]
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier.size(8.dp).clip(androidx.compose.foundation.shape.CircleShape)
                                                .background(color)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(nombre, color = AppTheme.TextMuted, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── Detalles de Cuotas ────────────────────────────────────
            if (state.cuotasSeries.isNotEmpty()) {
                item {
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Text(tr("est.detallesDeCuotas"), color = AppTheme.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(
                            tr("est.verTodasCuotas"),
                            color    = AppTheme.Indigo,
                            fontSize = 13.sp,
                            modifier = Modifier.clickable { onVerTodasCuotas() }
                        )
                    }
                }

                items(state.cuotasSeries.take(5)) { serie ->
                    CuotaSerieItem(
                        serie    = serie,
                        onClick  = { onVerDetalleCuota(serie.primerGastoId) }
                    )
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun GaugePainter(
    progress : Float,
    color    : Color,
    modifier : Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val strokeWidth = 18.dp.toPx()
        val radius      = (size.minDimension / 2) - strokeWidth
        val topLeft     = Offset(center.x - radius, center.y - radius)
        val arcSize     = Size(radius * 2, radius * 2)

        // Fondo
        drawArc(
            color       = Color(0xFF1F2937),
            startAngle  = 135f,
            sweepAngle  = 270f,
            useCenter   = false,
            style       = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            topLeft     = topLeft,
            size        = arcSize
        )
        // Progreso
        if (progress > 0f) {
            drawArc(
                color      = color,
                startAngle = 135f,
                sweepAngle = 270f * progress,
                useCenter  = false,
                style      = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                topLeft    = topLeft,
                size       = arcSize
            )
        }
    }
}

@Composable
private fun PieChartPainter(
    datos   : List<Triple<String, Double, String>>,
    total   : Double,
    modifier: Modifier = Modifier
) {
    val colores = listOf(
        AppTheme.Blue, AppTheme.Yellow, AppTheme.Green,
        AppTheme.Purple, AppTheme.Orange, AppTheme.Red
    )
    Canvas(modifier = modifier) {
        if (total <= 0) return@Canvas
        var startAngle = -90f
        datos.forEachIndexed { idx, (_, amount, colorHex) ->
            val color      = parsearColorStats(colorHex) ?: colores[idx % colores.size]
            val sweepAngle = (amount / total * 360f).toFloat()
            drawArc(
                color      = color,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter  = true,
                topLeft    = Offset.Zero,
                size       = size
            )
            startAngle += sweepAngle
        }
    }
}

@Composable
private fun CuotaSerieItem(
    serie  : EstadisticasViewModel.CuotaSerie,
    onClick: () -> Unit
) {
    val progreso = (serie.cuotaActual.toFloat() / serie.cuotaTotal.toFloat()).coerceIn(0f, 1f)

    Card(
        onClick = onClick,
        colors  = CardDefaults.cardColors(containerColor = AppTheme.CardDark),
        shape   = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(serie.descripcion, color = AppTheme.TextPrimary, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                Text(
                    formatMontoStats(serie.montoPorCuota),
                    color      = AppTheme.TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 14.sp
                )
            }
            Spacer(Modifier.height(4.dp))
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    tr("est.cuotaDe", serie.cuotaActual, serie.cuotaTotal),
                    color    = AppTheme.TextMuted,
                    fontSize = 12.sp
                )
                Text(
                    tr("est.restante", formatMontoStats(serie.montoRestante)),
                    color    = AppTheme.TextMuted,
                    fontSize = 12.sp
                )
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress   = { progreso },
                modifier   = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(50)),
                color      = AppTheme.Purple,
                trackColor = AppTheme.CardMedium
            )
        }
    }
}

@Composable
private fun GaugeBalanceCard(state: EstadisticasViewModel.UiState, modifier: Modifier) {
    Card(
        modifier = modifier,
        colors   = CardDefaults.cardColors(containerColor = AppTheme.CardDark),
        shape    = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier            = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(tr("est.balanceGastado"), color = AppTheme.TextMuted, fontSize = 12.sp)
            Spacer(Modifier.height(8.dp))
            Box(contentAlignment = Alignment.Center) {
                GaugePainter(
                    progress = state.porcentajeConsumo,
                    color    = if (state.porcentajeConsumo > 0.8f) AppTheme.Red else AppTheme.Yellow,
                    modifier = Modifier.size(150.dp)
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "${"%.1f".format(state.porcentajeConsumo * 100)}%",
                        color      = AppTheme.TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize   = 22.sp
                    )
                }
            }
            Text(
                tr("est.delIngresoMensual"),
                color    = AppTheme.TextMuted,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun PromedioMensualCard(state: EstadisticasViewModel.UiState, modifier: Modifier, fontSize: TextUnit) {
    Card(
        modifier = modifier,
        colors   = CardDefaults.cardColors(containerColor = AppTheme.CardDark),
        shape    = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier            = Modifier.padding(16.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(tr("est.promedioGastoMensual"), color = AppTheme.TextMuted, fontSize = 12.sp)
            Spacer(Modifier.height(12.dp))
            Text(
                formatMontoStats(state.promedioAnual),
                color      = AppTheme.TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize   = fontSize
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("⤵", color = AppTheme.Green, fontSize = 12.sp)
                Spacer(Modifier.width(4.dp))
                Text(
                    tr("est.datosAnioActual"),
                    color    = AppTheme.Green,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun formatMontoStats(monto: Double): String = Money.format(monto)

private fun parsearColorStats(hex: String?): Color? {
    if (hex == null) return null
    return try {
        val h = hex.removePrefix("#")
        Color(
            red   = h.substring(0, 2).toInt(16) / 255f,
            green = h.substring(2, 4).toInt(16) / 255f,
            blue  = h.substring(4, 6).toInt(16) / 255f
        )
    } catch (e: Exception) { null }
}