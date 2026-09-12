# Task 1 Report: Núcleo multi-hoja

Fecha: 2026-09-11

## Qué implementé

Reemplazo completo (`verbatim` del brief, salvo la desviación abajo) de 5 archivos para el núcleo multi-hoja:

1. `composeApp/src/commonMain/kotlin/com/angel/gg/domain/model/Exportacion.kt` — nuevos tipos: `HojaExcel(nombre, gastos, ingresoTotal)`; `ExportarDatos` ahora lleva `ingresos: List<IngresoMensual>` (antes `ingresoTotal: Double`); `ExportarContenido` ahora lleva `hojas: List<HojaExcel>` (antes hoja/gastos/ingresoTotal).
2. `composeApp/src/commonMain/kotlin/com/angel/gg/domain/usecase/ExportarGastosUseCase.kt` — devuelve `ingresos` (lista) en lugar de la suma; rango por clave `year*100+month`.
3. `composeApp/src/commonMain/kotlin/com/angel/gg/export/XlsxGenerador.kt` — genera N `sheetN.xml` + workbook/rels/contentTypes con `numHojas`, un único `sharedStrings.xml`.
4. `composeApp/src/commonMain/kotlin/com/angel/gg/presentation/viewmodel/ExportarViewModel.kt` — agrupa gastos por `(anio, mes)` ordenado ascendentemente; meses sin gastos NO generan hoja; `ingresoTotal` por hoja desde `IngresoMensual` del mes (0 si no existe); `guardar()` conserva el fix: `mensaje = if (ok) Strings.t("exportar.exito") else Strings.t("exportar.error")`, `esError = !ok`.
5. `composeApp/src/commonTest/kotlin/com/angel/gg/export/XlsxGeneradorTest.kt` — 7 tests (anexados: multi-hoja, sharedStrings compartido, G2 único por hoja con ingreso).

## Gates

### Gate 1: `.\gradlew.bat :composeApp:jvmTest`

Output verbatim (resumen):

```
> Task :composeApp:compileKotlinJvm
w: ... (warnings preexistentes del proyecto, no de Task 1)
> Task :composeApp:compileTestKotlinJvm
> Task :composeApp:jvmTest

BUILD SUCCESSFUL in 15s
16 actionable tasks: 5 executed, 11 up-to-date
Configuration cache entry reused.
```

Resultado en test-results: `tests=7 failures=0 errors=0 skipped=0` → **PASS (7/7)**.

Detalle: la primera ejecución FALLÓ por el error del brief (ver desviación). Tras aplicar `::idx`, jvmTest pasó.

### Gate 2: `.\gradlew.bat :composeApp:compileKotlinJvm :composeApp:compileDebugKotlinAndroid`

Output verbatim (resumen):

```
> Task :composeApp:compileKotlinJvm UP-TO-DATE
> Task :composeApp:compileDebugKotlinAndroid
w: ... (warnings preexistentes del proyecto)

BUILD SUCCESSFUL in 10s
25 actionable tasks: 2 executed, 23 up-to-date
Configuration cache entry reused.
```

→ **BUILD SUCCESSFUL**.

## Archivos cambiados

- `composeApp/src/commonMain/kotlin/com/angel/gg/domain/model/Exportacion.kt`
- `composeApp/src/commonMain/kotlin/com/angel/gg/domain/usecase/ExportarGastosUseCase.kt`
- `composeApp/src/commonMain/kotlin/com/angel/gg/export/XlsxGenerador.kt`
- `composeApp/src/commonMain/kotlin/com/angel/gg/presentation/viewmodel/ExportarViewModel.kt`
- `composeApp/src/commonTest/kotlin/com/angel/gg/export/XlsxGeneradorTest.kt`

Este reporte.

No se tocó ningún otro archivo. No es repo git (sin commits).

## Self-review

- Coherencia de tipos: `HojaExcel`/`ExportarContenido`/`ExportarDatos` usados consistentemente en los 5 archivos; `XlsxGenerador.generar(contenido: ExportarContenido)` y `ExportarViewModel.exportar/guardar/limpiarMensaje` mantienen firma pública esperada. `ExportarDatos`/`HojaExcel` definidos antes de su uso (mismo paquete `domain.model`), sin imports redundantes.
- `guardar()` conserva el fix de feedback: `mensaje = if (ok) Strings.t("exportar.exito") else Strings.t("exportar.error")`, `esError = !ok`.
- Agrupación por mes: `groupBy { anio to mes }` → `toSortedMap(compareBy { it.first * 100 + it.second })` → ordenado ascendentemente; meses sin gastos omitidos implícitamente; `ingresoDe` por hoja (`firstOrNull { anio && mes }?.monto ?: 0.0`).
- Generador: `hojasXml = contenido.hojas.mapIndexed` genera `sheet{1..N}.xml`; `contentTypesXml(numHojas)`, `workbookRelsXml(numHojas)`, `workbookXml(hojas)` con rIds escalados; un solo `sharedStrings.xml`. Tests `libroMultiHoja*` verifican hojas + G2 único + sharedStrings único.

## Desviaciones

1. **`XlsxGenerador.kt:27`** — el brief contiene `hojaXml(hoja, idx)` (función local pasada como valor sin `::`), que **no compila** en Kotlin: `Function invocation 'idx(...)' expected`. Cambio mínimo aplicado: `hojaXml(hoja, ::idx)`. Es un fix mecánico que preserva la intención exacta del brief; sin él la tarea era BLOCKED. Ningún otro cambio más allá del brief.

Sin desviaciones funcionales adicionales. Los warnings del build son preexistentes del proyecto (expect/actual beta, deprecated menuAnchor, etc.) y no pertenecen a los archivos de Task 1.