package com.angel.gg.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.angel.gg.domain.model.Gasto
import com.angel.gg.presentation.i18n.Strings
import com.angel.gg.presentation.i18n.tr
import com.angel.gg.presentation.viewmodel.BuscadorViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuscadorScreen(
    onVolver          : () -> Unit,
    onVerDetalleCuotas: (String) -> Unit,
    viewModel         : BuscadorViewModel = koinInject()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    OutlinedTextField(
                        value         = state.query,
                        onValueChange = viewModel::onQueryChange,
                        placeholder   = { Text(tr("buscador.placeholder")) },
                        singleLine    = true,
                        trailingIcon  = {
                            if (state.query.isNotBlank()) {
                                IconButton(onClick = viewModel::limpiar) {
                                    Icon(AppIcons.Limpiar, contentDescription = tr("buscador.limpiar"))
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(AppIcons.Volver, contentDescription = tr("comun.volver"))
                    }
                }
            )
        }
    ) { padding ->

        when {
            state.buscando -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            state.sinResultados -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        tr("buscador.sinResultados", state.query),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            state.resultados.isEmpty() && state.query.isBlank() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        tr("buscador.empezar"),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier            = Modifier.fillMaxSize().padding(padding),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(state.resultados, key = { it.id }) { gasto ->
                        ResultadoBusquedaItem(
                            gasto   = gasto,
                            onClick = {
                                if (gasto.esCuota) onVerDetalleCuotas(gasto.id)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultadoBusquedaItem(gasto: Gasto, onClick: () -> Unit) {
    Card(
        onClick  = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Row(
            modifier              = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(gasto.descripcion,     style = MaterialTheme.typography.bodyLarge)
                Text(gasto.categoriaNombre, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary)
                // Muestra la fecha y mes para ubicar el registro (Tarjeta 8, CA2)
                Text(
                    "${gasto.fecha} · ${Strings.mes(gasto.mes)} ${gasto.anio}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                formatMonto(gasto.monto),
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}