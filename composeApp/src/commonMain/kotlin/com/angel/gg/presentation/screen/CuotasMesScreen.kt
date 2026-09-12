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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.angel.gg.presentation.AppTheme
import com.angel.gg.presentation.i18n.Strings
import com.angel.gg.presentation.i18n.tr
import com.angel.gg.presentation.viewmodel.EstadisticasViewModel
import org.koin.compose.koinInject
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CuotasMesScreen(
    onVolver         : () -> Unit,
    onVerDetalleCuota: (String) -> Unit,
    viewModel        : EstadisticasViewModel = koinInject()
) {
    val state by viewModel.state.collectAsState()
    val hoy    = LocalDate.now()

    Scaffold(
        containerColor = AppTheme.BgDark,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(tr("cuotas.todasLasCuotas"), color = AppTheme.TextPrimary, fontWeight = FontWeight.Bold)
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

        if (state.cuotasSeries.isEmpty()) {
            Box(
                modifier         = Modifier.fillMaxSize().padding(padding).background(AppTheme.BgDark),
                contentAlignment = Alignment.Center
            ) {
                Text(tr("cuotas.sinCuotas"), color = AppTheme.TextSubtle, fontSize = 15.sp)
            }
            return@Scaffold
        }

        LazyColumn(
            modifier            = Modifier.fillMaxSize().padding(padding).background(AppTheme.BgDark),
            contentPadding      = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Card(
                    colors   = CardDefaults.cardColors(containerColor = AppTheme.CardDark),
                    shape    = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier              = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(tr("home.cuotasActivas", state.cuotasSeries.size), color = AppTheme.TextPrimary, fontWeight = FontWeight.Bold)
                        Text(
                            formatMonto(state.cuotasSeries.sumOf { it.montoPorCuota }),
                            color      = AppTheme.IndigoLight,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            items(state.cuotasSeries) { serie ->
                Card(
                    onClick  = { onVerDetalleCuota(serie.primerGastoId) },
                    colors   = CardDefaults.cardColors(containerColor = AppTheme.CardDark),
                    shape    = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            Text(serie.descripcion, color = AppTheme.TextPrimary, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                            Text(formatMonto(serie.montoPorCuota), color = AppTheme.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Spacer(Modifier.height(4.dp))
                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(tr("est.cuotaDe", serie.cuotaActual, serie.cuotaTotal), color = AppTheme.TextMuted, fontSize = 12.sp)
                            Text(tr("est.restante", formatMonto(serie.montoRestante)), color = AppTheme.TextMuted, fontSize = 12.sp)
                        }
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress   = { (serie.cuotaActual.toFloat() / serie.cuotaTotal).coerceIn(0f, 1f) },
                            modifier   = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(50)),
                            color      = AppTheme.Purple,
                            trackColor = AppTheme.CardMedium
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}