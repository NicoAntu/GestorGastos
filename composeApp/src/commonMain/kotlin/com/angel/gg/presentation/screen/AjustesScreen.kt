package com.angel.gg.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.angel.gg.presentation.AppTheme
import com.angel.gg.presentation.i18n.Lang
import com.angel.gg.presentation.i18n.Money
import com.angel.gg.presentation.i18n.Strings
import com.angel.gg.presentation.i18n.tr
import com.angel.gg.presentation.viewmodel.ExportarViewModel
import com.angel.gg.presentation.viewmodel.ImportarViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AjustesScreen(
    onVolver         : () -> Unit,
    onVerMisCategorias: () -> Unit
) {
    var abrirIdioma by remember { mutableStateOf(false) }
    var abrirMoneda  by remember { mutableStateOf(false) }
    var abrirExportar by remember { mutableStateOf(false) }
    val exportarViewModel: ExportarViewModel = koinInject()
    val exportarEstado by exportarViewModel.state.collectAsState()
    var abrirImportar by remember { mutableStateOf(false) }
    val importarViewModel: ImportarViewModel = koinInject()
    val importarEstado by importarViewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(exportarEstado.mensaje) {
        exportarEstado.mensaje?.let {
            snackbarHostState.showSnackbar(it)
            exportarViewModel.limpiarMensaje()
        }
    }

    LaunchedEffect(importarEstado.mensaje) {
        importarEstado.mensaje?.let {
            snackbarHostState.showSnackbar(it)
            importarViewModel.limpiarMensaje()
        }
    }

    Scaffold(
        containerColor = AppTheme.BgDark,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(tr("ajustes.titulo"), color = AppTheme.TextPrimary, fontWeight = FontWeight.Bold) },
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
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text(tr("ajustes.general"), color = AppTheme.TextMuted, fontSize = 12.sp)
            }
            item {
                Card(
                    colors   = CardDefaults.cardColors(containerColor = AppTheme.CardDark),
                    shape    = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AjustesRow(
                        AppIcons.Categorias,     tr("ajustes.misCategorias"), tr("ajustes.misCatSub"),
                        AppTheme.Indigo,         onClick = onVerMisCategorias
                    )
                    HorizontalDivider(color = AppTheme.BorderColor.copy(alpha = 0.4f))
                    Box {
                        Box(
                            Modifier
                                .align(Alignment.CenterEnd)
                                .width(100.dp)
                                .height(48.dp)
                        ) {
                            DropdownMenu(
                                expanded         = abrirIdioma,
                                onDismissRequest = { abrirIdioma = false },
                                containerColor   = AppTheme.CardDark
                            ) {
                                Lang.entries.forEach { lang ->
                                    DropdownMenuItem(
                                        text = { Text(lang.etiqueta, color = AppTheme.TextPrimary) },
                                        trailingIcon = {
                                            if (lang == Strings.langNow()) Text("✓", color = AppTheme.Indigo)
                                        },
                                        onClick = {
                                            Strings.setLang(lang)
                                            abrirIdioma = false
                                        }
                                    )
                                }
                            }
                        }
                        AjustesRow(
                            AppIcons.Idioma,         tr("ajustes.idioma"), tr("ajustes.idiomaSub"),
                            AppTheme.Blue,           valor = Strings.langNow().etiqueta,
                            onClick = { abrirIdioma = true }
                        )
                    }
                    HorizontalDivider(color = AppTheme.BorderColor.copy(alpha = 0.4f))
                    Box {
                        Box(
                            Modifier
                                .align(Alignment.CenterEnd)
                                .width(100.dp)
                                .height(48.dp)
                        ) {
                            DropdownMenu(
                                expanded         = abrirMoneda,
                                onDismissRequest = { abrirMoneda = false },
                                containerColor   = AppTheme.CardDark
                            ) {
                                Money.MONEDAS.forEach { simbolo ->
                                    DropdownMenuItem(
                                        text = { Text(simbolo, color = AppTheme.TextPrimary) },
                                        trailingIcon = {
                                            if (simbolo == Money.currencyNow()) Text("✓", color = AppTheme.Indigo)
                                        },
                                        onClick = {
                                            Money.setCurrency(simbolo)
                                            abrirMoneda = false
                                        }
                                    )
                                }
                            }
                        }
                        AjustesRow(
                            AppIcons.Moneda,         tr("ajustes.moneda"), tr("ajustes.monedaSub"),
                            AppTheme.Green,          valor = Money.currencyNow(),
                            onClick = { abrirMoneda = true }
                        )
                    }
                }
            }
            item {
                Text(tr("ajustes.datos"), color = AppTheme.TextMuted, fontSize = 12.sp)
            }
            item {
                Card(
                    colors   = CardDefaults.cardColors(containerColor = AppTheme.CardDark),
                    shape    = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AjustesRow(
                        AppIcons.Importar,       tr("ajustes.importar"), tr("ajustes.importarSub"),
                        AppTheme.Yellow,         onClick = { abrirImportar = true }
                    )
                    HorizontalDivider(color = AppTheme.BorderColor.copy(alpha = 0.4f))
                    AjustesRow(
                        AppIcons.Exportar,       tr("ajustes.exportar"), tr("ajustes.exportarSub"),
                        AppTheme.Orange,         onClick = { abrirExportar = true }
                    )
                }
            }
        }

        if (abrirExportar) {
            ExportarDialog(
                onDismiss = { abrirExportar = false },
                viewModel = exportarViewModel
            )
        }

        if (abrirImportar) {
            ImportarDialog(
                onDismiss = { abrirImportar = false },
                viewModel = importarViewModel
            )
        }
    }
}

@Composable
private fun AjustesRow(
    icono    : ImageVector,
    titulo   : String,
    subtitulo: String,
    tint     : Color,
    valor    : String?  = null,
    onClick  : () -> Unit
) {
    Row(
        modifier          = Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier         = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(tint.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icono, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(titulo, color = AppTheme.TextPrimary, fontWeight = FontWeight.Medium, fontSize = 15.sp)
            Text(subtitulo, color = AppTheme.TextMuted, fontSize = 12.sp)
        }
        if (valor != null) {
            Spacer(Modifier.width(10.dp))
            Text(valor, color = AppTheme.IndigoLight, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
        Icon(AppIcons.Siguiente, contentDescription = tr("ajustes.ir"), tint = AppTheme.TextSubtle)
    }
}