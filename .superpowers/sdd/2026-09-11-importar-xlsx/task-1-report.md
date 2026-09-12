# Task 1 Report — Núcleo lector (modelos + ZipReader + ImportarXlsx)

## 1. Status

**DONE_WITH_CONCERNS** — all gates pass, but the brief's verbatim `ImportarXlsx.kt` failed to compile against the project's Kotlin stdlib (2.3.20) and required a one-line adaptation (documented below). Everything else is byte-for-byte the brief.

## 2. Files created

- `composeApp/src/commonMain/kotlin/com/angel/gg/domain/model/Importacion.kt` — verbatim from brief (Step 3).
- `composeApp/src/commonMain/kotlin/com/angel/gg/export/ZipReader.kt` — verbatim from brief (Step 4).
- `composeApp/src/commonMain/kotlin/com/angel/gg/export/ImportarXlsx.kt` — from brief (Step 5) with ONE adaptation (see Self-review).
- `composeApp/src/commonTest/kotlin/com/angel/gg/export/ImportarXlsxTest.kt` — verbatim from brief (Step 1).

No existing files were modified. No dependencies added. No git operations (project is not a git repo).

## 3. Command evidence

### Step 2 — TDD red state (expected FAIL)

Command (workdir `D:\GestorGastos`):
`.\gradlew.bat :composeApp:jvmTest --tests "com.angel.gg.export.ImportarXlsxTest"`

Result: **BUILD FAILED** (correct red state). Excerpt of compiler errors:
```
e: file:///D:/GestorGastos/composeApp/src/commonTest/kotlin/com/angel/gg/export/ImportarXlsxTest.kt:3:34 Unresolved reference 'ResultadoImportacion'.
e: file:///.../ImportarXlsxTest.kt:96:26 Unresolved reference 'ImportarXlsx'.
e: ...81 lines mentioning Unresolved reference / Cannot infer type...
> Task :composeApp:compileTestKotlinJvm FAILED
BUILD FAILED in 3s
```
Failures are exactly the expected "types not defined yet" category.

### Step 6 — Test passes (after adaptation)

Command: `.\gradlew.bat :composeApp:jvmTest --tests "com.angel.gg.export.ImportarXlsxTest"`

Result: **BUILD SUCCESSFUL in 8s** — `:composeApp:jvmTest` ran, `ImportarXlsxTest` = **5 tests, 0 failures**.

First attempt at this step FAILED with `ImportarXlsx.kt:17:52 Return type mismatch: expected 'ImportarXlsx.ResultadoLectura', actual 'Any'` — see Self-review. After the one-line fix, it passed.

### Step 6 — Full suite

Command: `.\gradlew.bat :composeApp:jvmTest`

Result: **BUILD SUCCESSFUL in 1s**. Full JVM suite passes. JUnit XML reports:
```
com.angel.gg.export.ImportarXlsxTest: tests=5 failures=0 errors=0 skipped=0
com.angel.gg.export.XlsxGeneradorTest: tests=8 failures=0 errors=0 skipped=0
```

### Step 7 — Gates

Command: `.\gradlew.bat :composeApp:jvmTest :composeApp:compileKotlinJvm :composeApp:compileDebugKotlinAndroid`

Result: **BUILD SUCCESSFUL in 6s**. `:composeApp:jvmTest` UP-TO-DATE (passed), `:composeApp:compileKotlinJvm` passed, `:composeApp:compileDebugKotlinAndroid` passed (only pre-existing warnings: expect/actual Beta, elvis-on-nonnull, deprecated menuAnchor, opt-in DelicateDecomposeApi — none from the new files).

Total: **13 tests, 0 failures** (= 8 XlsxGeneradorTest + 5 ImportarXlsxTest), both compiles BUILD SUCCESSFUL.

## 4. Self-review / adaptations

**Adapted (1 line in `ImportarXlsx.kt`, in the `leer` body):**

Brief (verbatim) wrote:
```kotlin
fun leer(bytes: ByteArray): ResultadoLectura = runCatching {
    ...
    ResultadoImportacion(filas = filas, ingresosPorMes = ingresos)
}.getOrElse { ResultadoLectura.ArchivoInvalido }
```

This does not compile on Kotlin 2.3.20 (project's stdlib). `Result.getOrElse` has signature
`fun <R, C : R> Result<R>.getOrElse(onFailure: (Throwable) -> C): C` — the failure branch `C`
(here `ArchivoInvalido`) must be a subtype of the success type `R` (here `ResultadoImportacion`).
Since `ResultadoImportacion` and `ArchivoInvalido` are unrelated, inference collapses both type
variables to `Any`, producing `Return type mismatch: expected 'ResultadoLectura', actual 'Any'`
(liner error `ImportarXlsx.kt:17:52`).

Fix (minimal, semantically the only valid reading of the brief's own API): wrap the success value
in the brief-declared `ResultadoLectura.Ok`:
```kotlin
ResultadoLectura.Ok(ResultadoImportacion(filas = filas, ingresosPorMes = ingresos))
```
Now the `runCatching` block returns a `ResultadoLectura` subtype and `C : R` holds.
This matches the sealed interface declared verbatim in the brief:
`sealed interface ResultadoLectura { data class Ok(val datos: ResultadoImportacion); ... }` — the
`sad` path was always intended to be `Ok(datos)`, so the verbatim body simply omitted the `Ok`
wrapper. Signature, packages, sealed interfaces, helper functions, and ZipReader/Importacion are
all untouched.

Alternative rejected: casting the block result (`as ResultadoLectura`) would compile but throw
`ClassCastException` at runtime on every successful read — worse. Wrapping in `Ok` is correct.

**Files otherwise:** `Importacion.kt`, `ZipReader.kt`, and `ImportarXlsxTest.kt` created exactly
as written in the brief (verified copy). Consumed interfaces confirmed unchanged:
`ZipWriter.zip(List<Pair<String, ByteArray>>): ByteArray` (STORED, byte-aligned local headers — the
reader walks local headers with offset 0, which matches), `IngresoMensual(id, anio, mes, monto)`.

**Manual review of reader logic vs fixture/round-trip expectations:**
- `ZipReader.leer` walks local-file-header records and stops at the central directory signature; STORED
  method means `compSize` bytes follow the header with no data descriptor — matches `ZipWriter`.
- `ImportarXlsx.hojaDe` skips `r <= 1` (header row), Excel-serial dates, `E`-cell presence for cuota,
  and `G2` income. All 5 tests pass, including the `&`-in-shared-string case (`textos` list stores raw
  text; `construirLibro` escapes `&`→`&amp;` for XML; reader descodifica back to `&`).
- In `construirLibro`, the income cell is appended inside the loop when `r == 2`, so it lands in the
  first sheet's row 2; test 2 confirms the correct association (2026, 9, 3565.0) and that the sheet
  without income contributes nothing.

## 5. Concerns

1. **One-line deviation from the brief's verbatim code** (`Ok(...)` wrapper in `ImportarXlsx.kt:leer`).
   Required for the brief's own verification to pass on Kotlin 2.3.20; the brief's snippet as written
   is uncompilable. Recommended: correct the brief/task list so downstream implementers copy the fix,
   and note it in the SDD log. The rest of the file is untouched.
2. **Reminder for later tasks:** if a subsequent "persist" task needs the test's `G2` income-only sheet
   or a `ResultadoImportacion` with zero filas, decisions on how `SinGastos` (`ImportacionResultado`)
   is produced are still open — Task 1 does not emit it (out of scope).
3. Pre-existing warnings in the module (expect/actual Beta, deprecations) are unrelated to this task
   and were not touched.