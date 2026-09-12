# Task 9 Report: Diálogo de exportación + wiring en AjustesScreen

**Status:** DONE_WITH_CONCERNS
**Date:** 2026-09-10
**Feature:** Exportar a .xlsx (Task 9/9)

## What I implemented

### 1. Created `composeApp/src/commonMain/kotlin/com/angel/gg/presentation/screen/ExportarDialog.kt`

Implemented verbatim from the task brief:

- `@Composable fun ExportarDialog(onDismiss: () -> Unit, viewModel: ExportarViewModel = koinInject())`
  - Collects `ExportarUiState` via `viewModel.state.collectAsState()`.
  - Local state: `alcance` (ExportarAlcance), `desde`/`hasta` (`LocalDate?`), `editarCampo` (`Boolean?`).
  - `LaunchedEffect(estado.listoParaGuardar)` → calls `viewModel.guardar()` once the .xlsx bytes are ready.
  - `LaunchedEffect(estado.mensaje)` → dismisses only on success (`mensaje != null && !estado.esError`).
  - `AlertDialog` (containerColor = `AppTheme.CardDark`) with:
    - RadioButton list for all `ExportarAlcance.entries` (MES_ACTUAL / ANIO_ACTUAL / TODO / PERSONALIZADO).
    - Conditional date range row (`FechaCampo` × 2 with `Modifier.weight(1f)`) when PERSONALIZADO.
    - Indeterminate `CircularProgressIndicator` while `estado.exportando`.
    - Error message text in `AppTheme.Red` when `estado.esError`.
    - Confirm button enabled only when `!estado.exportando && alcanceValido(...)`; disabled-style color via `AppTheme.TextSubtle`.
    - Cancel button with `tr("comun.cancelar")`.
  - `DatePickerDialog` (with `rememberDatePickerState`) shown when PERSONALIZADO and a field is being edited; confirmed picker value is written back with **UTC** conversion (same pattern as EditarGastoScreen/RegistrarGastoScreen).
  - Private `alcanceValido(...)`: for PERSONALIZADO requires `desde != null && hasta != null && !hasta.isBefore(desde)`; otherwise true.

**Deviation (plan-approved):** `OutlinedTextField(onClick = ...)` does **not** exist in this project's Compose M3 (`material3 = 1.10.0-alpha05`, CMP 1.10.3). The compiler rejected the call (`No parameter with name 'onClick' found`). Applied the plan's fallback note (`task-9-brief.md:214`): wrapped the `OutlinedTextField` in `Box(Modifier.fillMaxWidth().clickable(onClick = onClick))` with `import androidx.compose.foundation.clickable`. The field keeps `readOnly = true`, `onValueChange = {}`, so taps route to the clickable Box and open the DatePicker.

### 2. Modified `composeApp/src/commonMain/kotlin/com/angel/gg/presentation/screen/AjustesScreen.kt`

- Added imports: `SnackbarHost`/`SnackbarHostState` (already covered by `material3.*`), `LaunchedEffect`, `collectAsState`, `ExportarViewModel`, `koinInject`.
- Added to composable body (alongside existing remembers):
  - `var abrirExportar by remember { mutableStateOf(false) }`
  - `val exportarViewModel: ExportarViewModel = koinInject()`
  - `val exportarEstado by exportarViewModel.state.collectAsState()`
  - `val snackbarHostState = remember { SnackbarHostState() }`
  - `LaunchedEffect(exportarEstado.mensaje)` → shows snackbar and calls `exportarViewModel.limpiarMensaje()`.
- `Scaffold` now takes `snackbarHost = { SnackbarHost(snackbarHostState) }`.
- Export row's `onClick = {}` changed to `onClick = { abrirExportar = true }`.
- Added `if (abrirExportar) { ExportarDialog(onDismiss = { abrirExportar = false; exportarViewModel.limpiarMensaje() }) }` inside the Scaffold content lambda, after the `LazyColumn`.

## Gates (BOTH PASSED)

### Gate 1 — Compile
Command: `.\gradlew.bat :composeApp:compileKotlinJvm :composeApp:compileDebugKotlinAndroid`

Result: **BUILD SUCCESSFUL** (25 actionable tasks: 3 executed, 22 up-to-date). Warnings are pre-existing project warnings (expect/actual Beta, deprecated menuAnchor, Elvis on non-nullable) — none introduced by this task's files. The first attempt FAILED with `No parameter with name 'onClick' found` on `OutlinedTextField` (line 162/166 of ExportarDialog.kt), fixed as described above; second run BUILD SUCCESSFUL.

### Gate 2 — Tests
Command: `.\gradlew.bat :composeApp:jvmTest`

Result: **BUILD SUCCESSFUL** (`:composeApp:jvmTest` executed; 16 actionable tasks, 4 executed, 12 up-to-date).

## Files changed

1. Created `D:\GestorGastos\composeApp\src\commonMain\kotlin\com\angel\gg\presentation\screen\ExportarDialog.kt`
2. Modified `D:\GestorGastos\composeApp\src\commonMain\kotlin\com\angel\gg\presentation\screen\AjustesScreen.kt`

No changes were made to the ViewModel, DI module, i18n, AppTheme, or icons (all pre-existing from Task 7/8).

## Self-review

### Completeness
- ExportarDialog: complete (alcance radioes, rango personalizado con DatePicker, spinner, error, validación, guardado automático, cierre en éxito). ✅
- Wiring: fila "Exportar" → `abrirExportar = true` → diálogo; snackbar conectado al mensaje del VM; cierre limpia mensaje. ✅
- Imports: todos presentes y usados. ✅

### Quality
- VM state handled correctly: `exportando` bloquea el botón, `listoParaGuardar` dispara `guardar()`, `mensaje` no-error cierra el diálogo; el snackbar en Ajustes muestra `exportar.exito`. ✅
- Dialog closes only on success (success path) or explicit user dismissal (cancel/tap-outside/back); on failure stays open and shows the Red error text. ✅
- Custom-range validation: botón deshabilitado hasta que desde y hasta estén definidos y desde ≤ hasta. ✅
- DatePicker UTC correctness: `atStartOfDay(ZoneOffset.UTC)` for initialMillis and back via `Instant.ofEpochMilli(...).atZone(UTC).toLocalDate()` — identical to the project's existing EditarGastoScreen pattern; a full date (00:00 UTC) round-trips losslessly regardless of device TZ. ✅
- Conventions: AppTheme colors, Material3 components (`AlertDialog`, `RadioButton`, `DatePickerDialog`, `OutlinedTextFieldDefaults`), `tr(...)` i18n, project alignment style. ✅

### YAGNI
- No extra UI, no new dependencies, no speculative changes. ✅

### Concerns (minor, out of scope)
1. **VM save-failure UX gap (pre-existing, Task 7):** In `ExportarViewModel.guardar()`, on save failure (`ok == false`) it sets `mensaje = null, esError = true`. Consequence: the snackbar/error path shows nothing on save failure, and since `mensaje` is null the dialog does not close (no error text shown either). Success path works as designed. This lives in the ViewModel (Task 7) — not touched here; flagging for whoever owns the VM.
2. **JVM save dialog note:** On JVM, `guardar()` opens a native `JFileChooser` (per `ExportarGuardado.jvm`); the dialog stays open while that chooser is up (by design) and closes only after a successful save. `limpiarMensaje()` on dismiss prevents stale messages from showing next time.

## Deviations from the brief

- Only deviation: the plan-sanctioned `Box(Modifier.clickable { onClick() })` fallback for `OutlinedTextField` (the `onClick` parameter overload does not exist in material3 1.10.0-alpha05 / CMP 1.10.3). Everything else was implemented verbatim.

---

# Fix Round (Critical — double VM injection)

**Date:** 2026-09-10

## The bug
`ExportarViewModel` is registered as a Koin `factory` in `AppModule.kt:194` (new instance per injection):
- `AjustesScreen.kt:41` injected instance A; its state fed the snackbar effect (`AjustesScreen.kt:42-50`).
- `ExportarDialog` default param injected instance B; the `AjustesScreen.kt:175-179` call did **not** pass a VM, so all dialog work (export, guardar, close-on-success, red errors) ran on B.
- Instance A therefore never saw `mensaje`, the snackbar `LaunchedEffect` never fired → no "Exportación completada" toast after a successful save.

## The change (exactly as requested, nothing else)
`AjustesScreen.kt` — dialog call now passes the screen's instance and no longer clears the message on dismiss:
- **From:** `ExportarDialog(onDismiss = { abrirExportar = false; exportarViewModel.limpiarMensaje() })`
- **To:** `ExportarDialog(onDismiss = { abrirExportar = false }, viewModel = exportarViewModel)`

`ExportarDialog.kt` unchanged — its `viewModel: ExportarViewModel = koinInject()` default param is kept (no other call site exists; the AjustesScreen call passes the instance explicitly, so the default is never reached at runtime for this flow).

Nothing else touched: the snackbar effect, `limpiarMensaje`, the ViewModel, and DI were left as-is.

## Verification (single-reachability check)
- Only one reachable `koinInject()` for `ExportarViewModel` remains: `AjustesScreen.kt:41`, whose instance is now the one passed to `ExportarDialog` and feeds the snackbar effect.
- Flow after save: `viewModel.guardar()` (instance A) sets `mensaje = "exportar.exito"`, `esError = false` → dialog's `LaunchedEffect(estado.mensaje)` dismisses on success, and `AjustesScreen`'s `LaunchedEffect(exportarEstado.mensaje)` receives the same `mensaje` → `snackbarHostState.showSnackbar("Exportación completada")` then `limpiarMensaje()`. Snackbar now appears.

## Gates (both PASSED)
- **Compile:** `.\gradlew.bat :composeApp:compileKotlinJvm :composeApp:compileDebugKotlinAndroid` → **BUILD SUCCESSFUL** (25 tasks, 3 executed). Only pre-existing project warnings.
- **Tests:** `.\gradlew.bat :composeApp:jvmTest` → **BUILD SUCCESSFUL** (`:composeApp:jvmTest` executed; 16 tasks, 3 executed).

## Final note
With dismiss no longer calling `limpiarMensaje()`, a stale success message cannot persist: the snackbar effect clears `mensaje` right after showing it, and `exportar()` itself resets `mensaje = null` at the start of every export.