package com.angel.gg.presentation.screen

import androidx.compose.foundation.background
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
import com.angel.gg.presentation.AppTheme
import com.angel.gg.presentation.i18n.Strings
import com.angel.gg.presentation.i18n.tr
import com.angel.gg.presentation.viewmodel.DashboardViewModel
import org.koin.compose.koinInject
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriaDetalleScreen(
    onVolver : () -> Unit,
    viewModel: DashboardViewModel = koinInject()
) {
    val state by viewModel.state.collectAsState()
    val hoy    = LocalDate.now()

    LaunchedEffect(Unit) {
        viewModel.cargarResumen(hoy.year, hoy.monthValue)
    }

    Scaffold(
        containerColor = AppTheme.BgDark,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(tr("categorias.titulo"), color = AppTheme.TextPrimary, fontWeight = FontWeight.Bold)
                        Text(
                            "${Strings.mes(hoy.monthValue)} ${hoy.year}",
                            color    = AppTheme.TextMuted,
                            fontSize = 12.sp
                        )
                    }
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

        if (state.cargando) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AppTheme.Indigo)
            }
            return@Scaffold
        }

        val resumen = state.resumen

        LazyColumn(
            modifier            = Modifier.fillMaxSize().padding(padding).background(AppTheme.BgDark),
            contentPadding      = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Total del mes
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = AppTheme.CardDark),
                    shape  = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier              = Modifier.padding(20.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(tr("categorias.totalGastado"), color = AppTheme.TextMuted, fontSize = 12.sp)
                            Text(
                                formatMonto(resumen?.totalGastado ?: 0.0),
                                color      = AppTheme.Red,
                                fontWeight = FontWeight.Bold,
                                fontSize   = 24.sp
                            )
                        }
                        if ((resumen?.ingresoMensual ?: 0.0) > 0) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(tr("categorias.ingreso"), color = AppTheme.TextMuted, fontSize = 12.sp)
                                Text(
                                    formatMonto(resumen?.ingresoMensual ?: 0.0),
                                    color      = AppTheme.Green,
                                    fontWeight = FontWeight.Bold,
                                    fontSize   = 24.sp
                                )
                            }
                        }
                    }
                }
            }

            // Lista de categorías
            if (resumen != null) {
                val total = resumen.totalGastado
                items(resumen.gastosPorCategoria.sortedByDescending { it.total }) { gc ->
                    val color = parsearColor(gc.categoriaColor) ?: AppTheme.Blue

                    Card(
                        colors   = CardDefaults.cardColors(containerColor = AppTheme.CardDark),
                        shape    = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(1.dp)) {
                            Row(
                                modifier              = Modifier.fillMaxWidth(),
                                verticalAlignment     = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(color.copy(alpha = 0.18f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        gc.categoriaNombre.take(1).uppercase(),
                                        color      = color,
                                        fontWeight = FontWeight.Bold,
                                        fontSize   = 16.sp
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier              = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(gc.categoriaNombre, color = AppTheme.TextPrimary, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                        Text(formatMonto(gc.total), color = AppTheme.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    }
                                    Spacer(Modifier.height(6.dp))
                                    LinearProgressIndicator(
                                        progress   = { if (total > 0) (gc.total / total).toFloat() else 0f },
                                        modifier   = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(50)),
                                        color      = color,
                                        trackColor = AppTheme.CardMedium
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        tr("categorias.porcentajeDelTotal", "%.1f".format(gc.porcentaje)),
                                        color    = AppTheme.TextMuted,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}