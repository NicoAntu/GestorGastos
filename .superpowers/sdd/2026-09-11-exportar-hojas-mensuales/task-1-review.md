# Task 1 Review: Núcleo multi-hoja

Reviewer: Task 1 Reviewer subagent
Date: 2026-09-11

## Files read

- 5 changed files (verbatim match check against brief)
- 7 dependents: ExportarDialog.kt, AjustesScreen.kt, ExportarGuardado.kt, ZipWriter.kt, Strings.kt, Gasto.kt, IngresoMensual.kt
- Brief, report, spec

## Findings

### INFO-1 — `::idx` deviation is correct (introduced by implementer)
- Severity: INFO
- File: `XlsxGenerador.kt:27`
- Description: The brief contains `hojaXml(hoja, idx)` (passing local function by name). In Kotlin, local functions require the `::` operator to be passed as a function reference. The implementer correctly changed this to `hojaXml(hoja, ::idx)`. This is a valid compile fix that preserves the brief's intent exactly. The report documents this deviation.

### INFO-2 — ExportarDialog.kt not modified (pre-existing, per scope)
- Severity: INFO
- File: `ExportarDialog.kt:156-180`
- Description: The DatePicker calendar fix (Task 2's scope) is correctly not included in Task 1. The `FechaCampo` component uses `Box(Modifier.clickable(...))` wrapping a `readOnly OutlinedTextField`, which is the known broken pattern documented in the spec. This is expected.

## Verification of behavior claims

| Claim | Verified | Evidence |
|-------|----------|----------|
| Multi-sheet: generar iterates hojas → sheet1.xml..sheetN.xml | Yes | `XlsxGenerador.kt:26-27` |
| workbook.xml has N sheets | Yes | `workbookXml:133-141` |
| content-types has N overrides | Yes | `contentTypesXml:112-126` |
| workbook rels rId1..N worksheets + rId(N+1) sharedStrings + rId(N+2) styles | Yes | `workbookRelsXml:144-155` |
| Single sharedStrings.xml unique across book | Yes | `textos` list built once in `generar`, shared across all `hojaXml` calls via `::idx` |
| ViewModel groups gastos by (year, monthValue) via LocalDate.parse | Yes | `ExportarViewModel.kt:66-70` |
| Sorted by month, omits months without gastos | Yes | `toSortedMap(compareBy { it.first*100+it.second })` at line 71; groupBy naturally omits empty groups |
| Per-month income (0.0 if none) | Yes | `ingresoDe` at line 80-81 |
| Sheet names "Ene 2026"-style via Strings.mes | Yes | `Strings.mes(anioMes.second)` at line 74; `Strings.mes(indice: Int)` confirmed at Strings.kt:73 |
| guardar() error-feedback fix preserved | Yes | `ExportarViewModel.kt:91-92` — `mensaje = if (ok) ... else Strings.t("exportar.error")` |
| generadorConIngresoCeroOmiteBalance (G2/I2/I3 omitted when ingresoTotal==0) | Yes | `XlsxGenerador.kt:79,83,89` — all guarded by `hoja.ingresoTotal > 0` |
| Balance/percentage behavior preserved when ingresoTotal>0 | Yes | `balanceDe` (line 102-103), `porcentajeDe` (line 105-110), row 3 appended (line 89-91) |
| Tests: 7 tests | Yes | 7 `@Test` functions confirmed in XlsxGeneradorTest.kt |

## Verbatim match check

- Exportacion.kt: **MATCH** (lines 1-24 identical to brief)
- ExportarGastosUseCase.kt: **MATCH** (lines 1-49 identical to brief)
- XlsxGenerador.kt: **MATCH** except line 27 `::idx` (documented deviation)
- ExportarViewModel.kt: **MATCH** (lines 1-110 identical to brief)
- XlsxGeneradorTest.kt: **MATCH** (lines 1-110 identical to brief)

## Integration check

- `Gasto` model (Gasto.kt:3-22): fields match brief; `esCuota` getter present
- `IngresoMensual` model (IngresoMensual.kt:3-8): fields `id`, `anio`, `mes`, `monto` match brief
- `Strings.mes(indice: Int)` (Strings.kt:73): 1-based, returns `meses(langNow()).getOrElse(indice - 1)`
- `ZipWriter.zip(List<Pair<String, ByteArray>>)` (ZipWriter.kt:10): signature matches brief
- `ExportarGuardado.guardar(nombreSugerido: String, bytes: ByteArray): Boolean` (ExportarGuardado.kt:4): expect class, signature matches brief
- `ExportarDialog.kt`: not modified, uses `ExportarViewModel` via koinInject — no integration issues
- `AjustesScreen.kt`: not modified, uses `ExportarViewModel` — no integration issues

## Gates

| Gate | Result |
|------|--------|
| `.\gradlew.bat :composeApp:jvmTest` | **BUILD SUCCESSFUL** — 7/7 tests pass (0 failures, 0 errors, 0 skipped) |
| `.\gradlew.bat :composeApp:compileKotlinJvm :composeApp:compileDebugKotlinAndroid` | **BUILD SUCCESSFUL** — both JVM + Android compile clean |

## Summary

| Severity | Count |
|----------|-------|
| BLOCK | 0 |
| FLAG | 0 |
| INFO | 2 |

The implementation is a faithful verbatim reproduction of the brief with one necessary compile fix (`::idx`). All behavior claims verified against actual file contents. Both gates pass. No integration issues with dependent files. Task 1 is complete and correct.
