# Final Whole-Branch Review — Exportar hojas mensuales + fix DatePicker

Fecha: 2026-09-11
Entorno: NO-GIT (sin diffs/commits; verificado leyendo los archivos directamente)
Branch: `docs/superpowers/plans/2026-09-11-exportar-hojas-mensuales.md`
Archivos leídos: spec, plan, task-1/2 reports & reviews, ledger previo, y TODOS los archivos fuente/test/listado.

## VERDICT: READY TO SHIP

## Verificación por objetivo

| # | Objetivo | Estado | Evidencia |
|---|----------|--------|-----------|
| 1 | Generación multi-hoja | ✅ | `XlsxGenerador.generar` emite N `sheet{1..N}.xml` (`mapIndexed`, XlsxGenerador.kt:26-27); `workbookXml` con N `<sheet name/ sheetId/ r:id>` (133-142); `contentTypesXml(numHojas)` con N overrides de worksheet (112-126); `workbookRelsXml` rId1..N worksheets + rId(N+1) sharedStrings + rId(N+2) styles (144-155); `sharedStrings.xml` ÚNICO compartido (textos global acuulado vía `::idx`, una sola entrada en el zip, línea 35). |
| 2 | Agrupación por mes | ✅ | `hojasDe` parsea `LocalDate.parse(gasto.fecha)` → `(year, monthValue)` (ExportarViewModel.kt:67-69); `toSortedMap(compareBy { it.first*100 + it.second })` orden cronológico asc (71); `groupBy` omite meses sin gastos implícitamente; nombre `"${Strings.mes(mes)} $anio"` (74) con `Strings.mes` 1-based (Strings.kt:73); `ingresoDe` → `?.monto ?: 0.0` (80-81). |
| 3 | Guardrail ingreso 0 | ✅ | G2 solo si `r==2 && ingresoTotal>0` (XlsxGenerador.kt:79-81); I2 igual (83-85); fila I3 solo si `>0` (89-91); con ingreso>0 emite G2 + `G2-SUM(A:A)` + `FIXED((I2/G2)*100.2)&"%"` (comportamiento previo preservado). Test `generadorConIngresoCeroOmiteBalance` cubre el caso 0. |
| 4 | Flujo completo | ✅ | Confirm → `viewModel.exportar(alcance, desde, hasta)` (ExportarDialog.kt:114); `guardar()` conserva fix de feedback: `mensaje = if (ok) exito else error`, `esError = !ok` (ExportarViewModel.kt:92-94); `limpiarMensaje` intacto (99-101); AjustesScreen crea UN solo `exportarViewModel` (AjustesScreen.kt:41) y lo pasa al diálogo (178) — fix snackbar previo intacto; `LaunchedEffect(estado.mensaje)` del diálogo (43-46) y snackbar de Ajustes (45-50) coherentes con VM compartido. |
| 5 | Fix calendario | ✅ | `FechaCampo` ahora es `Surface(onClick = onClick, shape=RoundedCornerShape(4.dp), color=CardDark, border=BorderStroke(1.dp, BorderColor))` con `Row(etiqueta + valor/placeholder + Icon(AppIcons.Calendario))` (ExportarDialog.kt:155-192); firma `(etiqueta, valor, modifier, onClick)` intacta — call sites sin cambios (81-88, trailing lambda); `androidx.compose.foundation.clickable` NO está en imports; `Box` también removido (coherente, deviación documentada); sin text field readOnly que consuma el click → `editarCampo` se setea → `DatePickerDialog` se abre (126-152). Sin warnings de imports sin usar (0 warnings en ExportarDialog.kt en ambos gates). |
| 6 | Sin regresiones | ✅ | Sin claves i18n nuevas: `registrar.seleccionarFecha` preexistente en Es/En/Fr (EsStrings.kt:110, EnStrings.kt:103, FrStrings.kt:103); `exportar.*` del feature previo intactas. Sin dependencias nuevas (solo Compose Foundation ya existente: BorderStroke/RoundedCornerShape). Ajustes intacto (rows, idioma/moneda, snackbar). Query `obtenerPorRango` del use case sin cambios (ExportarGastosUseCase.kt:36) → filtro en rango intacto. Modelos Gasto/IngresoMensual/ZipWriter/ExportarGuardado sin tocar y compatibles. |
| 7 | Tests | ✅ | 7 tests coherentes con la API nueva (`ExportarContenido(nombreArchivo, hojas)`); `ZipWriter` usa método STORED (ZipWriter.kt:25) → asserts por `contains` sobre binario son fiables. 7/7 PASS en ejecución fresca. |

## Hallazgos

| Severidad | Hallazgo | file:line |
|-----------|----------|-----------|
| BLOCK | — | — |
| FLAG | — | — |
| INFO-1 | `::idx` (fix mecánico de compilación del brief; correcto, documentado) | XlsxGenerador.kt:27 |
| INFO-2 | Import `Box` removido junto con `clickable` (necesario para gates sin imports sin usar; correcto) | ExportarDialog.kt:3-22 |
| INFO-3 | `@OptIn(ExperimentalMaterial3Api::class)` redundante en `FechaCampo` (ya presente a nivel archivo línea 24; verbatim del brief, compila sin warning) | ExportarDialog.kt:155 |
| INFO-4 | Verificación manual GUI (clic Desde/Hasta en Desktop) pendiente de humano (entorno sin GUI) | ExportarDialog.kt:165-190 |

## Gates (ejecutados frescos por el revisor final)

### `.\gradlew.bat :composeApp:jvmTest --rerun-tasks`
```
Reusing configuration cache.
> Task :composeApp:kmpPartiallyResolvedDependenciesChecker
...
> Task :composeApp:jvmTest

BUILD SUCCESSFUL in 8s
16 actionable tasks: 16 executed
Configuration cache entry reused.
```
Resultado en `build/test-results/jvmTest/TEST-com.angel.gg.export.XlsxGeneradorTest.xml`:
`testsuite name="com.angel.gg.export.XlsxGeneradorTest" tests="7" skipped="0" failures="0" errors="0"`

### `.\gradlew.bat :composeApp:compileKotlinJvm :composeApp:compileDebugKotlinAndroid --rerun-tasks`
```
> Task :composeApp:compileKotlinJvm
w: ... (warnings preexistentes del proyecto: expect/actual Beta, menuAnchor deprecated, Elvis op)
> Task :composeApp:compileDebugKotlinAndroid
w: ... (mismos warnings preexistentes + expect/actual android)

BUILD SUCCESSFUL in 8s
25 actionable tasks: 25 executed
Configuration cache entry reused.
```
Cero warnings en los archivos cambiados de esta iteración.

## Conclusión

El núcleo multi-hoja (modelo → use case → ViewModel → generador) implementa fielmente el spec: N worksheets con `sharedStrings` único, agrupación cronológica por mes omitiendo meses sin gastos, ingreso por mes con guardrail G2/I2/I3, y el fix de `FechaCampo` con el patrón `Surface(onClick=…)` probado en Registrar/Editar. Sin claves i18n ni dependencias nuevas. Todo el flujo de UI (VM único, feedback de guardado, snackbar) intacto. Ambos gates pasan frescos (7/7 tests + BUILD SUCCESSFUL JVM/Android). Único pendiente: verificación manual GUI del DatePicker por un humano.