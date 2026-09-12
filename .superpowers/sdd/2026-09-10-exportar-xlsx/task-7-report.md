# Task 7 Report: ExportarViewModel + DI

**Status:** DONE

## What I implemented

### Step 1 — `ExportarViewModel.kt` (created)
`composeApp/src/commonMain/kotlin/com/angel/gg/presentation/viewmodel/ExportarViewModel.kt`

- `data class ExportarUiState(exportando, listoParaGuardar, bytes, nombreArchivo, mensaje, esError)`.
- `ExportarViewModel(exportarGastosUseCase, exportarGuardado) : ViewModel()` with `state: StateFlow<ExportarUiState>`.
- `exportar(alcance, desde, hasta)`: resets state, `runCatching { ejecutar(...) }`; on empty gastos → `Strings.t("exportar.sinDatos")` + `esError`; on success builds `ExportarContenido` (hoja/nombreArchivo from the private `hojaDe`/`archivoDe` scoping helpers), calls `XlsxGenerador.generar`, stores bytes + flags `listoParaGuardar`; on failure → `Strings.t("exportar.error")`.
- `guardar()`: early-returns if no bytes/name, calls `exportarGuardado.guardar(nombre, bytes)` in `viewModelScope`, clears the ready-to-save state, sets `exportar.exito` or `null` message + `esError = !ok`.
- `limpiarMensaje()`: nulls message and resets `esError`.
- `hojaDe`/`archivoDe` naming per scope: MES_ACTUAL → month+year (sheet) / "Gastos <mes> <año>.xlsx", ANIO_ACTUAL → year, TODO → "Gastos.xlsx", PERSONALIZADO → "Gastos <desde> a <hasta>.xlsx".

Used verbatim code, with one allowed clean-up: imported `ExportarDatos` cleanly (`com.angel.gg.domain.model.ExportarDatos`) instead of the brief's fully-qualified reference, per brief note (line 130) and to match `EditarGastoViewModel`'s model-import convention. Imports sorted alphabetically (`androidx.*` → `com.angel.gg.*` → `java.time` → `kotlinx.coroutines.*`), matching the file pattern.

### Step 2 — `AppModule.kt` (modified)
`composeApp/src/commonMain/kotlin/com/angel/gg/di/AppModule.kt`

- `domainModule`: appended `factory { ExportarGastosUseCase(gastoRepository = get(), ingresoMensualRepository = get()) }` after `GestionarCategoriaUseCase` (per brief), with arg columns aligned to file style.
- `viewModelModule`: appended `factory { ExportarViewModel(exportarGastosUseCase = get(), exportarGuardado = get()) }` after `EditarGastoViewModel`, with aligned args.
- Imports added in alphabetical order: `com.angel.gg.domain.usecase.ExportarGastosUseCase` (line 16), `com.angel.gg.export.ExportarGuardado` (line 23), `com.angel.gg.presentation.viewmodel.ExportarViewModel` (line 32).
- `ExportarGuardado` resolves via the platform Koin singles already registered in `DesktopModule` (jvm) and `AndroidModule` (android).

## Gate output

Command: `.\gradlew.bat :composeApp:compileKotlinJvm :composeApp:compileDebugKotlinAndroid --console=plain`

Result: **BUILD SUCCESSFUL** in 6s (25 actionable tasks: 3 executed, 22 up-to-date). All compiler warnings are pre-existing (expect/actual beta, deprecated menuAnchor, etc.); no warnings reference the new files.

## Files changed

- Created: `composeApp/src/commonMain/kotlin/com/angel/gg/presentation/viewmodel/ExportarViewModel.kt`
- Modified: `composeApp/src/commonMain/kotlin/com/angel/gg/di/AppModule.kt`

## Self-review findings

- **Completeness:** Full VM implemented; DI registered in both `domainModule` and `viewModelModule`; imports alphabetical and matching file style. All consumed signatures verified against the real code before writing (`ExportarGastosUseCase.ejecutar`, `ExportarDatos(desde/hasta/alcance/gastos/ingresoTotal)`, `XlsxGenerador.generar(ExportarContenido): ByteArray`, `ExportarGuardado.guardar(nombre, bytes): Boolean`, `Strings.t`/`Strings.mes` 1-based). Nothing needed adaptation.
- **Quality:** Error handling via `runCatching` on the use case; `listoParaGuardar` flow (true only after bytes ready, cleared after save); `limpiarMensaje` clears both message and error flag; hoja/archivo naming follows scope in `MES_ACTUAL`/`ANIO_ACTUAL`/`TODO`/`PERSONALIZADO`.
- **YAGNI:** No extra code, fields, or dependencies added.
- **Minor notes (not acted on, out of task scope):**
  1. `XlsxGenerador.generar(...)` is invoked inside `onSuccess` (after `runCatching` completed), so a generator failure would escape into `viewModelScope` rather than route to `exportar.error`. Kept verbatim per brief.
  2. i18n keys `exportar.sinDatos`, `exportar.error`, `exportar.exito` are **not yet present** in `EsStrings.kt`/`EnStrings.kt`/`FrStrings.kt` (only `ajustes.exportar`/`ajustes.exportarSub` exist). They resolve to the raw key at runtime until a later task adds them. No compile impact; flagged so the UI/strings task picks them up.

## Deviations

- Only one, intentional and permitted by the brief (note line 130): clean `ExportarDatos` import instead of fully-qualified reference. Nothing else deviated.

---

# Fix Round 1 (2026-09-10)

## Finding fixed (Important)

`XlsxGenerador.generar(contenido)` ran inside `onSuccess` but **outside** the `runCatching`. A throw during generation (e.g. `LocalDate.parse(fechaIso)` on a malformed `gasto.fecha`, or a `ZipWriter.zip` failure) escaped the coroutine uncaught: `exportando` stayed `true` forever (permanent spinner), no error message shown, and on Android an uncaught exception in `viewModelScope` could crash the app.

## What I changed

`composeApp/src/commonMain/kotlin/com/angel/gg/presentation/viewmodel/ExportarViewModel.kt` — restructured `exportar(...)` so the **entire** pipeline lives inside a single `runCatching`: `exportarGastosUseCase.ejecutar(...)` → empty-gastos check → `ExportarContenido` build → `hojaDe`/`archivoDe` → `XlsxGenerador.generar` → success `_state.update`. The single `.onFailure` now covers any exception from the use case **or** the generation step, clearing `exportando`, setting `mensaje = Strings.t("exportar.error")` and `esError = true`. The empty-gastos → `exportar.sinDatos` + `esError` path is preserved unchanged. The two deferred Minor findings (i18n keys; generator-failure path per the original brief) were intentionally NOT touched, per instructions.

Behavior reasoning (evidence, no unit tests cover this class):
- Success: same as before — bytes + `nombreArchivo` set, `listoParaGuardar = true`, `exportando = false`.
- Empty gastos: `SinDatos` message, `exportando = false`, `esError = true`.
- Failure (use case OR generator): `.onFailure` fires → `exportando = false`, error message, `esError = true`. No uncaught exception can escape `viewModelScope`.

## Gate output

Command: `.\gradlew.bat :composeApp:compileKotlinJvm :composeApp:compileDebugKotlinAndroid --console=plain`

Result: **BUILD SUCCESSFUL** in 6s (25 actionable tasks: 3 executed, 22 up-to-date). All warnings are pre-existing (expect/actual Beta, deprecated `menuAnchor`, pre-existing elvis warning); none reference the changed code.

---

# Fix Round 2 — guardar() failure path (2026-09-10)

## Finding fixed (Important, from Task 9 review)

On save failure (`ok == false`: user cancels the native JFileChooser/SAF dialog, or I/O error), `guardar()` set `mensaje = null`. `ExportarDialog.kt` renders red error text only when `esError && mensaje != null`, and its `LaunchedEffect(estado.mensaje)` / screen snackbar never fire — the user got zero feedback on a cancelled/failed save. The dialog was designed to show `exportar.error` inline, but the VM never published it.

## Change

One line in `ExportarViewModel.kt` (`guardar()`), line 74:

```kotlin
// before
mensaje = if (ok) Strings.t("exportar.exito") else null,
// after
mensaje = if (ok) Strings.t("exportar.exito") else Strings.t("exportar.error"),
```

Everything else untouched (`exportar()`, `limpiarMensaje()`, `hojaDe`, `archivoDe`, the approved runCatching structure).

## Gate results

- `.\gradlew.bat :composeApp:jvmTest` → **BUILD SUCCESSFUL** (js tests pass; 16 actionable tasks).
- `.\gradlew.bat :composeApp:compileKotlinJvm :composeApp:compileDebugKotlinAndroid` → **BUILD SUCCESSFUL** (5s; only pre-existing warnings).

## Self-check

Re-read of `guardar()` (lines 65-79) confirms: `ok == false` → `mensaje = Strings.t("exportar.error")` with `esError = true` (inline error + LaunchedEffect + snackbar all now fire); `ok == true` → `mensaje = Strings.t("exportar.exito")` with `esError = false` (success message unchanged). Both branches keep `listoParaGuardar = false, bytes = null, nombreArchivo = null`.