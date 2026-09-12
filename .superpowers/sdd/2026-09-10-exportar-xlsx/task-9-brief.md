# Task 9 Brief: Diálogo de exportación + wiring en AjustesScreen

Extraído del plan: `D:\GestorGastos\docs\superpowers\plans\2026-09-10-exportar-xlsx.md` (Task 9, líneas 1105-1371). Este archivo es tu única fuente de requisitos; usa los valores exactos verbatim.

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/angel/gg/presentation/screen/ExportarDialog.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/angel/gg/presentation/screen/AjustesScreen.kt`

**Interfaces:**
- Consumes: `ExportarViewModel` (Task 7, con `state: StateFlow<ExportarUiState>` y `exportar(alcance, desde, hasta)`, `guardar()`, `limpiarMensaje()`), `ExportarAlcance` (Task 2), `tr(...)`/`Strings.t(...)`, `AppTheme.*`, `AppIcons.Exportar`.
- Produces:
  - `@Composable fun ExportarDialog(onDismiss: () -> Unit, viewModel: ExportarViewModel = koinInject())`.
  - AjustesScreen: fila "Exportar" abre el diálogo; SnackbarHost muestra `mensaje` del VM.

## Paso previo: leer el estado real (verificado para ti)

- **AjustesScreen.kt** (191 líneas): `AjustesScreen(onVolver, onVerMisCategorias)`. Tiene `Scaffold(containerColor = AppTheme.BgDark, topBar = { TopAppBar... }) { padding -> LazyColumn(...) { ... } }`. La fila Exportar está en `AjustesRow(AppIcons.Exportar, tr("ajustes.exportar"), tr("ajustes.exportarSub"), AppTheme.Orange, onClick = {})` (líneas 151-154) dentro del último Card. Imports actuales incluyen `androidx.compose.material3.*`, `androidx.compose.runtime.*` (Composable, getValue, mutableStateOf, remember, setValue), `tr`. NO incluye SnackbarHost/SnackbarHostState/LaunchedEffect/collectAsState/koinInject/ExportarViewModel.
- **AppTheme.kt**: todos los colores usados existen (BgDark, CardDark, BorderColor, Indigo, IndigoLight, TextPrimary, TextMuted, TextSubtle, Green, Red, Yellow, Blue, Orange).
- **i18n**: existe `comun.cancelar` (ES "Cancelar"). `exportar.*` ya agregadas (Task 8).

## Step 1: Implementar ExportarDialog

`composeApp/src/commonMain/kotlin/com/angel/gg/presentation/screen/ExportarDialog.kt`:

```kotlin
package com.angel.gg.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.angel.gg.domain.model.ExportarAlcance
import com.angel.gg.presentation.AppTheme
import com.angel.gg.presentation.i18n.tr
import com.angel.gg.presentation.viewmodel.ExportarViewModel
import java.time.LocalDate
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportarDialog(
    onDismiss: () -> Unit,
    viewModel: ExportarViewModel = koinInject()
) {
    val estado by viewModel.state.collectAsState()

    var alcance by remember { mutableStateOf(ExportarAlcance.MES_ACTUAL) }
    var desde by remember { mutableStateOf<LocalDate?>(null) }
    var hasta by remember { mutableStateOf<LocalDate?>(null) }
    var editarCampo by remember { mutableStateOf<Boolean?>(null) } // true=desde, false=hasta

    // Una vez generados los bytes, disparar el guardado (JFileChooser o SAF)
    LaunchedEffect(estado.listoParaGuardar) {
        if (estado.listoParaGuardar) viewModel.guardar()
    }

    // Cerrar el diálogo al confirmar el guardado
    LaunchedEffect(estado.mensaje) {
        val m = estado.mensaje
        if (m != null && !estado.esError) onDismiss()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppTheme.CardDark,
        title = { Text(tr("exportar.titulo"), color = AppTheme.TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ExportarAlcance.entries.forEach { opcion ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = alcance == opcion,
                            onClick = { alcance = opcion },
                            colors = RadioButtonDefaults.colors(selectedColor = AppTheme.Indigo)
                        )
                        Text(
                            when (opcion) {
                                ExportarAlcance.MES_ACTUAL -> tr("exportar.mesActual")
                                ExportarAlcance.ANIO_ACTUAL -> tr("exportar.anioActual")
                                ExportarAlcance.TODO -> tr("exportar.todo")
                                ExportarAlcance.PERSONALIZADO -> tr("exportar.personalizado")
                            },
                            color = AppTheme.TextPrimary,
                            fontSize = 15.sp
                        )
                    }
                }

                if (alcance == ExportarAlcance.PERSONALIZADO) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        FechaCampo(
                            etiqueta = tr("exportar.desde"), valor = desde,
                            modifier = Modifier.weight(1f)
                        ) { editarCampo = true }
                        FechaCampo(
                            etiqueta = tr("exportar.hasta"), valor = hasta,
                            modifier = Modifier.weight(1f)
                        ) { editarCampo = false }
                    }
                }

                if (estado.exportando) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(8.dp),
                            color = AppTheme.Indigo
                        )
                    }
                }

                estado.mensaje?.let {
                    if (estado.esError) {
                        Text(it, color = AppTheme.Red, fontSize = 13.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !estado.exportando && alcanceValido(alcance, desde, hasta),
                onClick = { viewModel.exportar(alcance, desde, hasta) }
            ) {
                Text(tr("exportar.boton"), color = if (estado.exportando) AppTheme.TextSubtle else AppTheme.Indigo)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(tr("comun.cancelar"), color = AppTheme.TextMuted)
            }
        }
    )

    if (alcance == ExportarAlcance.PERSONALIZADO && editarCampo != null) {
        val campo = editarCampo
        val fechaInicial = if (campo == true) desde else hasta
        val millisInicial = fechaInicial?.atStartOfDay(java.time.ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
        val state = rememberDatePickerState(initialSelectedDateMillis = millisInicial)
        DatePickerDialog(
            onDismissRequest = { editarCampo = null },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        val fecha = java.time.Instant.ofEpochMilli(millis)
                            .atZone(java.time.ZoneOffset.UTC)
                            .toLocalDate()
                        if (campo == true) desde = fecha else hasta = fecha
                    }
                    editarCampo = null
                }) { Text("OK", color = AppTheme.Indigo) }
            },
            dismissButton = {
                TextButton(onClick = { editarCampo = null }) {
                    Text(tr("comun.cancelar"), color = AppTheme.TextMuted)
                }
            }
        ) {
            DatePicker(state = state)
        }
    }
}

@Composable
private fun FechaCampo(
    etiqueta: String,
    valor: LocalDate?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(modifier = modifier) {
        Text(etiqueta, color = AppTheme.TextMuted, fontSize = 12.sp)
        OutlinedTextField(
            value = valor?.toString() ?: "",
            readOnly = true,
            onValueChange = {},
            onClick = onClick,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AppTheme.Indigo,
                unfocusedBorderColor = AppTheme.BorderColor,
                focusedTextColor = AppTheme.TextPrimary,
                unfocusedTextColor = AppTheme.TextPrimary
            )
        )
    }
}

private fun alcanceValido(
    alcance: ExportarAlcance,
    desde: LocalDate?,
    hasta: LocalDate?
): Boolean = when (alcance) {
    ExportarAlcance.PERSONALIZADO -> desde != null && hasta != null && !hasta.isBefore(desde)
    else -> true
}
```

Nota del plan: `OutlinedTextField(onClick=...)` es API M3 reciente (1.10 alpha): si el compilador no la acepta, envolver el `OutlinedTextField` en un `Box(Modifier.clickable { onClick() })` (con `import androidx.compose.foundation.clickable`). El `Modifier.weight(1f)` está dentro de un `Row` (RowScope) — compila.

## Step 2: Wire en AjustesScreen

Modificar `AjustesScreen.kt`:

1. Al comienzo del body de `AjustesScreen`, junto a los `remember` actuales, añadir:

```kotlin
    var abrirExportar by remember { mutableStateOf(false) }
    val exportarViewModel: ExportarViewModel = koinInject()
    val exportarEstado by exportarViewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(exportarEstado.mensaje) {
        exportarEstado.mensaje?.let {
            snackbarHostState.showSnackbar(it)
            exportarViewModel.limpiarMensaje()
        }
    }
```

2. En el `Scaffold` de `AjustesScreen`, añadir el snackbarHost:

```kotlin
    Scaffold(
        containerColor = AppTheme.BgDark,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { ... }   // sin cambios
    ) { padding -> ...
```

3. Cambiar el `onClick = {}` de la fila Exportar:

```kotlin
                    AjustesRow(
                        AppIcons.Exportar,       tr("ajustes.exportar"), tr("ajustes.exportarSub"),
                        AppTheme.Orange,         onClick = { abrirExportar = true }
                    )
```

4. Justo antes del cierre del `Scaffold` (después del `LazyColumn`, dentro del `Scaffold` content lambda — en el bloque `) { padding -> ... }` después del `LazyColumn` y antes del `}` que cierra el lambda del content), añadir:

```kotlin
            if (abrirExportar) {
                ExportarDialog(
                    onDismiss = { abrirExportar = false; exportarViewModel.limpiarMensaje() }
                )
            }
```

5. Imports nuevos en AjustesScreen.kt:

```kotlin
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import com.angel.gg.presentation.viewmodel.ExportarViewModel
import org.koin.compose.koinInject
```

## Step 3: Gate de compilación + tests completos

Run: `.\gradlew.bat :composeApp:jvmTest`
Run: `.\gradlew.bat :composeApp:compileKotlinJvm :composeApp:compileDebugKotlinAndroid`
Expected: PASS + BUILD SUCCESSFUL.

## Environment / Global Constraints

- Working directory: `D:\GestorGastos`. Work from there (workdir) para todos los comandos.
- IMPORTANT: Este proyecto NO es un repositorio git. No uses git ni commits.
- No agregar dependencias nuevas. `org.koin.compose.koinInject` debe estar disponible (mira si otros archivos del proyecto lo usan; si no, verifica que koin-compose esté en las dependencias — el proyecto usa Koin con `koinInject` probablemente en HomeScreen; si NO compila, reporta BLOCKED con lo que hay).
- Sigue las convenciones de UI del proyecto (AppTheme, Material3).

## Your Job

1. Implementa Steps 1-2 exactamente.
2. Gates (Step 3).
3. Self-review: completeness (ExportarDialog completo, wiring, snackbar, imports), calidad (estado del VM manejado, cierre de diálogo solo en éxito, validez de rango personalizado, DatePicker UTC correcto), YAGNI.
4. Report.

## When You're in Over Your Head

Siempre está bien parar y reportar BLOCKED/NEEDS_CONTEXT con detalles. No adivines (p.ej. si `koinInject` no existe en el proyecto o si `OutlinedTextField(onClick)` no compila — en ese caso aplica la nota del plan).

## Report Format

Report file: `D:\GestorGastos\.superpowers\sdd\2026-09-10-exportar-xlsx\task-9-report.md`
Include: qué implementaste, output de ambos gates, archivos cambiados, self-review findings, desviaciones.

Then report back with ONLY (under 15 lines):
- **Status:** DONE | DONE_WITH_CONCERNS | BLOCKED | NEEDS_CONTEXT
- One-line gates summary (tests + compile)
- Your concerns, if any
- The report file path

If BLOCKED or NEEDS_CONTEXT, put the specifics in the final message itself.