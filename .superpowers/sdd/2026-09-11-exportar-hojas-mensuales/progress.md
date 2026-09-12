# SDD ledger — plan: docs/superpowers/plans/2026-09-11-exportar-hojas-mensuales.md

Proyecto: D:\GestorGastos (NO es repo git — sin commits; gates: :composeApp:jvmTest + compileKotlinJvm + compileDebugKotlinAndroid).
Workspace: D:\GestorGastos\.superpowers\sdd\2026-09-11-exportar-hojas-mensuales\
Adaptación: revisores leen brief + report + archivos directamente (sin diffs git).

Estado:
- [x] Task 1: Núcleo multi-hoja — implementer DONE_WITH_CONCERNS (`::idx` fix mecánico, documentado en task-1-report.md); reviewer DONE, 0 BLOCK 0 FLAG 2 INFO; gates 7/7 PASS + BUILD SUCCESSFUL
- [x] Task 2: Fix FechaCampo — implementer DONE (`Box` import removido por coherencia, documentado); reviewer DONE, 0 BLOCK 0 FLAG 3 INFO; gates 7/7 + BUILD SUCCESSFUL; verificación manual GUI pendiente de humano
- [x] Final whole-branch review — READY TO SHIP (0 BLOCK 0 FLAG 4 INFO: ::idx fix, Box import removal, @OptIn redundante, verificación GUI pendiente de humano); gates fresh 7/7 + BUILD SUCCESSFUL

## Bug post-ship (usuario: "hojas vacías")

- **Causa raíz:** en el refactor multi-hoja, las entradas del zip eran `sheet${i+1}.xml` (raíz) en vez de `xl/worksheets/sheet${i+1}.xml` (XlsxGenerador.kt:27). `workbook.xml.rels` apunta a `worksheets/sheetN.xml` (→ xl/worksheets/sheetN.xml), que ya no existía → Excel/WPS mostraba las pestañas pero hojas vacías. El test `libroMultiHojaCreaUnaSheetPorMes` no lo detectó: el assert `contains("xl/worksheets/sheet1.xml")` hacía match con el string del Content_Types, no con el entry real del zip.
- **Fix:** `"xl/worksheets/sheet${i + 1}.xml"` + test nuevo `libroMultiHojaUbicaLasHojasBajoXlWorksheets` (parsea nombres de entradas del zip con `nombresZip()`). TDD: rojo confirmado antes del fix. Gates: 8/8 tests PASS + BUILD SUCCESSFUL (jvmTest/compileKotlinJvm/compileDebugKotlinAndroid).
- Plan actualizado (entry path + helper `nombresZip` + test nuevo). Pendiente humano: verificación manual en desktop (abrir xlsx con datos por hoja).