# Task 5 Report: expect `ExportarGuardado` + actual JVM (Desktop)

Date: 2026-09-10
Status: DONE

## What was implemented

Implemented the platform save mechanism for the "Export to .xlsx" feature, using the
project's declarable expect/actual pattern (mirrors `SettingsStore`):

1. **Step 1 — expect declaration (commonMain)**:
   `composeApp/src/commonMain/kotlin/com/angel/gg/export/ExportarGuardado.kt`
   - `expect class ExportarGuardado { suspend fun guardar(nombreSugerido: String, bytes: ByteArray): Boolean }`
   - Verbatim from the brief.

2. **Step 2 — JVM actual (jvmMain)**:
   `composeApp/src/jvmMain/kotlin/com/angel/gg/export/ExportarGuardado.jvm.kt`
   - `actual class ExportarGuardado` with `Dispatchers.Swing` + `JFileChooser`.
   - Save dialog titled "Exportar xlsx", `.xlsx` file name extension filter, auto-appends
     `.xlsx` when the chosen file lacks the extension, writes bytes with `runCatching`.
   - Returns `false` on cancel or write failure (contract per brief).
   - Verbatim from the brief.

## Pattern reference (pre-step)

Reviewed `SettingsStore.kt` (expect, commonMain) / `SettingsStore.jvm.kt` (jvmMain) /
`SettingsStore.android.kt` (androidMain) to respect declaration style and file placement.
New files follow the same conventions: same package layout, `expect`/`actual` classes,
`.jvm.kt` suffix in `jvmMain`.

## Gate output

Command: `.\gradlew.bat :composeApp:compileKotlinJvm`

```
> Task :composeApp:compileKotlinJvm
w: ... expect/actual classes ... are in Beta ... (matches pre-existing SettingsStore/DatabaseDriverFactory warnings)
w: ... pre-existing warnings (GastoRepositoryImpl, RootComponent, menuAnchor deprecation) ...
BUILD SUCCESSFUL in 5s
9 actionable tasks: 2 executed, 7 up-to-date
```

The only new warning is the `expect`/`actual` classes Beta warning — identical class of
warning already emitted by the existing `SettingsStore` and `DatabaseDriverFactory` files,
so it is consistent with project status quo and not introduced by this task.

## Files changed

- Created `composeApp/src/commonMain/kotlin/com/angel/gg/export/ExportarGuardado.kt`
- Created `composeApp/src/jvmMain/kotlin/com/angel/gg/export/ExportarGuardado.jvm.kt`

(Also created the `jvmMain/.../export` directory, which did not previously exist.)

## Dependency check

`kotlinx.coroutinesSwing` confirmed present in jvmMain dependencies
(`composeApp/build.gradle.kts` line 66, via version catalog). No new dependencies added.

## Self-review

- **Completeness**: Both steps implemented verbatim; gate green. Contract
  (cancel/no-target → `false`) satisfied by `else false` and `runCatching`.isSuccess.
- **Quality**: Mirrors existing SettingsStore conventions; no comments added; no extra
  files; no tests required per brief (platform UI; gate is compilation).
- **YAGNI**: Exactly the two declared files, nothing more. Android actual intentionally
  deferred to Task 6 and NOT implemented here.

## Deviations

None. `kotlinx.coroutines.swing.Swing` was available and compiled without issue.