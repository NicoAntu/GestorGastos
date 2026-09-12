# Importación de gastos desde archivo (.xlsx) — Design Spec

Fecha: 2026-09-11
Estado: Aprobado por el usuario

## 1. Objetivo

Permitir en Ajustes → Importar seleccionar un archivo `.xlsx` **exportado por la propia app** y cargar en la base de datos los gastos (y los ingresos mensuales que el archivo contenga). Es el inverso de la exportación existente.

**Fuera de alcance (explícito):** lectura de `.xls` binario (BIFF), archivos `.xlsx` arbitrarios de Excel/WPS con estructura distinta, y reconstrucción de series de cuotas vinculadas (`idPadre`). Las filas con cuota se importan como gastos planos.

## 2. Decisiones clave (confirmadas con el usuario)

| Pregunta | Decisión |
|----------|----------|
| Formato | Solo archivos exportados por la app (`.xlsx`), round-trip determinista |
| Duplicados | Saltar filas que ya existen (mismo monto + descripcion + categoria + fecha) |
| Categorías | Recrear la categoría por nombre (sin distinguir may./min.) si no existe |
| Ingresos mensuales | Restaurar el `IngresoMensual` que aparece en cada hoja (G2) |
| Parser | Parser propio en KMP (ZipReader + scanner XML), sin dependencias nuevas |

## 3. Arquitectura

Espejo de la arquitectura de exportación:

```
presentation/ImportarDialog  →  ImportarViewModel (koin)
                                      │
                    reading            │
                    ┌──────────────────▼──────────────┐
                    │  export/ImportarXlsx (común)     │  ZipReader + scanner XML
                    └──────────────────┬──────────────┘
                                       │ ResultadoImportacion
                                       ▼
                    domain/usecase/ImportarGastosUseCase → repos (*)
                                       │ ResumenImportacion
                                       ▼
                              snackbar (AjustesScreen)
```

(*) `GastoRepository`, `CategoriaRepository`, `IngresoMensualRepository`, `GestionarCategoriaUseCase`.

- `expect class ImportarLectura` / actuals: lee el archivo desde disco (JVM: `JFileChooser` abrir; Android: SAF `OpenDocument`). `suspend fun leer(): ByteArray?` → `null` si el usuario cancela.

## 4. Componentes y flujo de datos

### 4.1 `export/ZipReader.kt` (común, puro)

Inverso de `ZipWriter`: recorre los local headers del zip (método STORED, flag UTF-8) y devuelve `List<Pair<String, ByteArray>>` (nombre de entrada → bytes). Si el layout no es STORED/coherente → error "archivo no válido". Repite offset por `30 + fnameLen + extraLen + compSize`.

### 4.2 `export/ImportarXlsx.kt` (común, puro)

`leer(bytes: ByteArray): ResultadoLectura`, donde:

```kotlin
sealed interface ResultadoLectura {
    data class Ok(val datos: ResultadoImportacion) : ResultadoLectura
    data object ArchivoInvalido : ResultadoLectura
}
```

1. Desempaqueta con `ZipReader` y localiza `xl/workbook.xml` (orden de hojas) y `xl/_rels/workbook.xml.rels` (ruta real de cada hoja by r:id) y `xl/sharedStrings.xml` (textos por índice).
2. Por cada hoja (`xl/worksheets/sheetN.xml`):
   - Ignora la fila 1 (encabezados).
   - Por cada fila lee: `A` = monto (numérico), `B` = descripción (shared string), `C` = categoría (shared string), `D` = fecha (serial numérico → `LocalDate` con epoch base `1899-12-30`), `E` = cuota (texto, opcional).
   - `esCuota = true` si `E` no está vacía.
   - Si la hoja tiene celda `G2` (ingreso del mes, `> 0`), registra un ingreso para el `(anio, mes)` deducido de los gastos de esa hoja.
3. Descodifica entidades XML básicas: `&amp;` `&lt;` `&gt;` `&quot;` `&apos;`.

### 4.3 `domain/model/Importacion.kt`

```kotlin
data class FilaImportada(
    val monto: Double,
    val descripcion: String,
    val categoriaNombre: String,
    val fecha: LocalDate,
    val esCuota: Boolean
)

data class ResultadoImportacion(
    val filas: List<FilaImportada>,
    val ingresosPorMes: List<IngresoMensual>
)

data class ResumenImportacion(
    val nuevos: Int,
    val omitidos: Int,
    val categoriasCreadas: Int,
    val ingresosRestaurados: Int
)

sealed interface ImportacionResultado {
    data class Exito(val resumen: ResumenImportacion) : ImportacionResultado
    data object SinGastos : ImportacionResultado
}
```

### 4.4 `domain/usecase/ImportarGastosUseCase.kt`

Entrada: `ResultadoImportacion`.

1. **Pre-validación:** `filas` vacía → `ImportacionResultado.SinGastos`.
2. **Dedupe:** `minFecha`/`maxFecha` de las filas → `gastoRepository.obtenerPorRango(desde, hasta)`. Claves `(monto, descripcion.trim().lowercase(), categoriaNombre.trim().lowercase(), fecha.toString())`. Las repetidas cuentan como omitidas.
3. **Categorías:** `categoriaRepository.obtenerTodas().first()` → mapa `nombre.lowercase() → Categoria`. Si un nombre no existe → `gestionarCategoriaUseCase.crear(nombre, null, "#4F46E5", "default", false)`; de su `Resultado.Exito(categoria)` se toma la categoría **resuelta** (existente o creada), que aporta `id`, `nombre`, `color`, `icono` a cada `Gasto`. Un `Resultado.Fallo` se propaga como `importar.error`.
4. **Inserción:** por cada fila nueva: `Gasto(generarId(), monto, descripcion, categoriaId, categoriaNombre, categoriaColor, categoriaIcono, fecha.toString(), anio=fecha.year, mes=fecha.monthValue)` sin campos de cuota → `gastoRepository.insertar`.
5. **Ingresos:** por cada `IngresoMensual` del resultado con `monto > 0`: `ingresoMensualRepository.guardar(...)` (upsert, no duplica). Nota: un archivo puede traer gastos de meses distintos; cada `G2` de hoja se asocia a su mes.
6. Devuelve `ImportacionResultado.Exito(resumen)`.

### 4.5 `presentation/viewmodel/ImportarViewModel.kt` (koin)

- `importar()`: `importarLectura.leer()` → si `null` (cancelado), no muestra nada. Con bytes:
  - `ImportarXlsx.leer` → `ResultadoLectura.ArchivoInvalido` → `importar.archivoNoValido` (esError).
  - Use case → `SinGastos` → `importar.sinGastos` (esError); `Exito` → mensaje con resumen (nuevos / omitidos / categorías creadas / ingresos).
  - Excepción → `importar.error` (esError).
- `limpiarMensaje()`.

### 4.6 `presentation/screen/ImportarDialog.kt` + wiring

- Espejo de `ExportarDialog` (Material3, panel oscuro, botones Importar/Cerrar), con spinner mientras corre.
- `AjustesScreen`: cambiar `onClick = {}` de la fila Importar (AjustesScreen.kt:164) por `abrirImportar = true`, añadir estado y el diálogo (espejo del bloque `if (abrirExportar) { ExportarDialog(...) }`).

### 4.7 `export/ImportarLectura` (expect/actual)

- `jvmMain`: `JFileChooser.showOpenDialog`, filtro "Excel (.xlsx)", devuelve bytes o `null`.
- `androidMain`: puente `ImportarPuente` (launcher `ActivityResultContracts.OpenDocument` con MIME `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`, URI-persistente no necesario), registrado en `MainActivity` (espejo de `ExportarPuente`). Lee bytes vía `contentResolver`.

### 4.8 i18n

Claves nuevas `importar.*` en ES/EN/FR (estructura como `exportar.*`):
`importar.titulo`, `importar.boton` (etiqueta del botón "Seleccionar archivo"), `importar.exito` ("Se importaron {0} gastos ({1} ya existían, {2} categorías creadas, {3} ingresos)"), `importar.error`, `importar.archivoNoValido`, `importar.sinGastos`.

La fila de Ajustes ya tiene `ajustes.importar` / `ajustes.importarSub` (no cambiar).

## 5. Manejo de errores

| Caso | Comportamiento |
|------|----------------|
| Usuario cancela el diálogo de archivo | Sin mensaje; sin cambios |
| Zip no válido / estructura inesperada | `importar.archivoNoValido` (esError) |
| Archivo válido pero 0 filas | `importar.sinGastos` (esError) |
| Falla de repositorio al insertar | `importar.error` (esError) |
| Éxito | Resumen (nuevos/omitidos/categorías/ingresos) |

## 6. Testing

- **`ImportarXlsxTest`** (commonTest, usa `ZipWriter` para fabricar libros de prueba con el formato real):
  - Parseo de filas: montos, descripciones, categorías, fechas serial → `LocalDate`.
  - Decodificación de entidades XML en shared strings.
  - Hoja con `G2` → ingreso por (anio, mes); hoja sin `G2` → sin ingreso.
  - Fila con cuota → `esCuota = true`.
  - Zip inválido → error "archivo no válido".
- **`ImportarGastosUseCaseTest`** (commonTest, repos falsos):
  - Dedupe: misma clave ya en rango → omitido; misma fila en otro mes → nuevo.
  - Categoría desconocida → creada una sola vez y reutilizada (mismo id) en filas siguientes.
  - Ingreso con monto > 0 → guardado (upsert); no duplica.
  - Conteos del `ResumenImportacion` correctos.

## 7. Gates y convenciones

- Proyecto sin git: sin commits.
- Gates por tarea: `.\gradlew.bat :composeApp:jvmTest` (PASS) y `.\gradlew.bat :composeApp:compileKotlinJvm :composeApp:compileDebugKotlinAndroid` (BUILD SUCCESSFUL).
- Sin dependencias nuevas; `java.time` en commonMain; `Strings.t(...)`/`tr(...)`; `java.util`/`javax.swing` solo en `jvmMain`, `android.*` solo en `androidMain`.
- No hay refactor de `ZipWriter` ni de `XlsxGenerador` (se mantienen intactos).