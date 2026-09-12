# Task 2 Review: Fix DatePicker del rango Personalizado (FechaCampo)

Reviewer: Task 2 Reviewer subagent
Date: 2026-09-11
Environment: NO-GIT (verified by reading files directly; no diffs/commits available)

## Files read

- Task 2 brief: `task-2-brief.md`
- Task 1 review (pre-change state of `ExportarDialog.kt`): `task-1-review.md`
- Implementer report: `task-2-report.md`
- Spec: `docs/superpowers/specs/2026-09-11-exportar-hojas-mensuales-design.md`
- Changed file: `ExportarDialog.kt` (full, 201 lines)
- `AppIcons.kt` (full, 38 lines)
- `EsStrings.kt` / `EnStrings.kt` / `FrStrings.kt` (searched, line ~110/103/103)

## Verbatim match check — `FechaCampo` block

The block at `ExportarDialog.kt:155-192` matches the brief's Step 2 block (brief lines 33-71)
**verbatim**, line by line:

- `@OptIn(ExperimentalMaterial3Api::class)` — brief:33 / file:155
- `private fun FechaCampo(etiqueta: String, valor: LocalDate?, modifier: Modifier = Modifier, onClick: () -> Unit)` — brief:35-39 / file:157-161
- `Column(modifier = modifier) { Text(etiqueta, color = AppTheme.TextMuted, fontSize = 12.sp) }` — brief:41-42 / file:163-164
- `Surface(onClick, shape = RoundedCornerShape(4.dp), color = AppTheme.CardDark, border = BorderStroke(1.dp, AppTheme.BorderColor), modifier = Modifier.fillMaxWidth())` — brief:43-49 / file:165-171
- `Row(padding(horizontal = 12.dp, vertical = 10.dp).fillMaxWidth(), SpaceBetween, CenterVertically)` — brief:50-56 / file:172-178
- `Text(valor?.toString() ?: tr("registrar.seleccionarFecha"), color = if (valor == null) TextSubtle else TextPrimary, fontSize = 14.sp)` — brief:57-61 / file:179-183
- `Icon(AppIcons.Calendario, contentDescription = tr("registrar.seleccionarFecha"), tint = AppTheme.TextMuted)` — brief:62-66 / file:184-188

**Signature intact.** The two call sites (`ExportarDialog.kt:81-84` Desde, `:85-88` Hasta) are
unchanged — positional `FechaCampo(etiqueta, valor, modifier) { … }` with trailing `onClick` lambda,
exactly as before.

## Findings

### INFO-1 — `Box` import removed (deviation, introduced by implementer)
- Severity: INFO
- File: `ExportarDialog.kt:3-22`
- Description: Brief Step 1 lists only: remove `foundation.clickable`, add `foundation.BorderStroke`
  and `foundation.shape.RoundedCornerShape`. The implementer additionally removed
  `import androidx.compose.foundation.layout.Box`. Verified correct: after the replacement there is
  **no** `Box` reference anywhere in the file (verified with `findstr` — zero matches for both
  `clickable` and `Box`). Keeping it would have produced an unused-import warning, contradicting the
  brief's own gate criterion ("sin warnings de imports sin usar"). Correct, necessary deviation;
  documented in the report.

### INFO-2 — Manual GUI verification (Step 4) not run (per brief allowance)
- Severity: INFO
- File: `ExportarDialog.kt:165-190`
- Description: Step 4 (`:composeApp:run` + clicking Desde/Hasta) requires an interactive GUI.
  Report documents it as pending human verification, which the brief explicitly permits ("Si no
  tienes GUI, documentar como pendiente de verificación humana"). Static verification confirms the
  fix: the click-consuming `OutlinedTextField(readOnly)` is gone; the new `Surface(onClick=…)`
  (M3 overload) is wired directly to `onClick` → `editarCampo = true/false` → `DatePickerDialog`
  (`:126-152`). The pattern is identical to the working RegistrarGasto/EditarGasto implementation.

### INFO-3 — Redundant `@OptIn(ExperimentalMaterial3Api::class)` (from brief)
- Severity: INFO
- File: `ExportarDialog.kt:155`
- Description: The file already has `@OptIn(ExperimentalMaterial3Api::class)` at file/function scope
  (line 24, pre-existing on `ExportarDialog`). The brief's block adds another on the private
  `FechaCampo` (line 155). Harmless redundancy, applied verbatim as the brief states. Compiles
  without warning.

## Verification checklist (brief steps)

| Check | Result | Evidence |
|-------|--------|----------|
| `clickable` import removed and unreferenced | Pass | no `clickable` anywhere in file (findstr, exit 1) |
| `BorderStroke` added and used | Pass | `ExportarDialog.kt:3`, used at `:169` |
| `RoundedCornerShape` added and used | Pass | `ExportarDialog.kt:9`, used at `:167` |
| All imports used by new block present | Pass | `Text/Icon/Surface/ExperimentalMaterial3Api` via `material3.*` (:10); `Column/Row` (:5-6); `Arrangement` (:4); `fillMaxWidth` (:7); `padding` (:8); `Alignment` (:13); `Modifier` (:14); `dp` (:15); `sp` (:16); `AppTheme` (:18); `tr` (:19); `LocalDate` (:21); `AppIcons` same package |
| No unused imports remain (whole file) | Pass | every explicit import :3-22 used ≥1 time; `Box` removed; fresh compile produced no import warnings |
| No new i18n keys | Pass | new block only uses `registrar.seleccionarFecha`, which exists in Es/En/Fr strings (EsStrings.kt:110, EnStrings.kt:103, FrStrings.kt:103) and is used with identical `tr(...)` call shape in RegistrarGastoScreen.kt:219 / EditarGastoScreen.kt:168 |
| Only localized change; dialog/wiring untouched | Pass | `AlertDialog` (:48-124), radio options, `DatePickerDialog` block (:126-152), `alcanceValido` (:194-201) unchanged vs Task 1 state; call sites identical |
| `AppIcons.Calendario` exists | Pass | `AppIcons.kt:37` — `val Calendario = Icons.Filled.DateRange` |

## Gates (run fresh by this reviewer, NOT up-to-date)

| Gate | Command | Result |
|------|---------|--------|
| Tests | `.\gradlew.bat :composeApp:jvmTest --rerun-tasks` | **BUILD SUCCESSFUL** — 16 tasks executed; `XlsxGeneradorTest`: **7 tests, 0 failures, 0 errors, 0 skipped** (verified in `build/test-results/jvmTest/TEST-com.angel.gg.export.XlsxGeneradorTest.xml`) |
| Compile | `.\gradlew.bat :composeApp:compileKotlinJvm :composeApp:compileDebugKotlinAndroid --rerun-tasks` | **BUILD SUCCESSFUL** — 25 tasks executed (both compile tasks forced fresh) |

Warnings in output are all pre-existing and NOT from this file: `expect/actual` Beta
(DatabaseDriverFactory, SettingsStore, ExportarGuardado), `menuAnchor()` deprecated
(RegistrarGastoScreen.kt:187, EditarGastoScreen.kt:134), `DelicateDecomposeApi`
(RootComponent.kt:67), `?:` always-left (GastoRepositoryImpl.kt:41). **Zero warnings for
`ExportarDialog.kt`.**

## Summary

| Severity | Count |
|----------|-------|
| BLOCK | 0 |
| FLAG | 0 |
| INFO | 3 |

The implementation is a faithful verbatim reproduction of the brief's `FechaCampo` replacement.
The signature and both call sites are unchanged; the click-consuming text field is eliminated in
favor of the M3 `Surface(onClick=…)` pattern proven in RegistrarGasto/EditarGasto. The one
deviation (`Box` import removal) is necessary and correctly documented. i18n reuses the existing
`registrar.seleccionarFecha` key in all three locales. Both gates pass on fresh execution with no
warnings from the changed file. Task 2 is complete and correct.