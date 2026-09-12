# Diseño: Exportar gastos a .xlsx

Fecha: 2026-09-10
Estado: Aprobado por el usuario (2026-09-10)

## Objetivo

Implementar la fila "Exportar" de Ajustes (actualmente `onClick = {}`) para generar un archivo
`.xlsx` real (OOXML) con los gastos, replicando la estructura del ejemplo que proporcionó el usuario
en `D:\GestorGastos\Ejemplo.xlsx` (hoja "Sep 2026", 7 gastos, fórmulas de balance y porcentaje).

## Alcance

- **Formato**: `.xlsx` moderno (OOXML), no `.xls` legacy ni CSV.
- **Alcances de exportación (4)**:
  - Mes actual → gastos del mes de hoy; ingreso = `IngresoMensual` de ese mes.
  - Año actual → gastos del año de hoy; ingreso = suma de ingresos de los meses del año.
  - Todo → todos los gastos; ingreso = suma de todos los ingresos mensuales.
  - Personalizado → gastos entre dos fechas elegidas (desde/hasta); ingreso = suma de ingresos
    de los meses comprendidos en el rango.
- **Primera iteración**: tabla + columnas de saldo/balance + porcentaje, **sin gráfico circular**
  (el gráfico del ejemplo se aborda en una iteración futura).
- **Guardado**: selector de destino por plataforma (diálogo Guardar en Desktop, SAF/`CreateDocument`
  en Android).

## Arquitectura

Componentes en capas, siguiendo el patrón existente del proyecto (repositorios → use cases → ViewModel → UI):

```
AjustesScreen (Dialog)
        │  ExportarViewModel (factory en Koin)
        ▼
ExportarGastosUseCase ──► GastoRepository.obtenerPorRango()
        │                   IngresoMensualRepository.obtenerEnRango()
        ▼
ExportarContenido(gastos: List<Gasto>, ingresoTotal: Double)
        │
        ▼
XlsxGenerador.generar(contenido): ByteArray   [commonMain, Kotlin puro]
        │
        ▼
guardarXlsx(nombreSugerido, bytes): resultado   [expect/actual]
        ├─ Desktop: JFileChooser + File.writeBytes
        └─ Android: CreateDocument (SAF) + contentResolver
```

### Capa de datos

Nuevas queries SQLDelight:

- `Gasto.sq` → `obtenerPorRango`:
  ```sql
  SELECT g.*, c.nombre AS categoria_nombre, c.color AS categoria_color, c.icono AS categoria_icono
  FROM Gasto g
  INNER JOIN Categoria c ON g.categoria_id = c.id
  WHERE g.fecha >= ? AND g.fecha <= ?
  ORDER BY g.fecha ASC, g.rowid ASC;
  ```
  La columna `fecha` se guarda como `YYYY-MM-DD`, por lo que el orden lexicográfico
  (y la comparación de rango) es cronológico. `ORDER BY fecha ASC` replica el orden del ejemplo
  (ascendente).
- `IngresoMensual.sq` → `obtenerEnRango`:
  ```sql
  SELECT * FROM IngresoMensual
  WHERE (anio * 100 + mes) >= ? AND (anio * 100 + mes) <= ?;
  ```
  El rango de meses se deriva de las fechas desde/hasta (año*100+mes).

Nuevo método en cada interfaz de repositorio y su implementación correspondiente
(`GastoRepository.obtenerPorRango(desde: String, hasta: String)` y
`IngresoMensualRepository.obtenerEnRango(desdeAnioMes: Long, hastaAnioMes: Long)`).

### Use case y modelo

- `ExportarContenido(gastos: List<Gasto>, ingresoTotal: Double)` (nuevo modelo).
- `ExportarGastosUseCase` (factory en `AppModule`) con un `enum ExportarAlcance { MES_ACTUAL, ANIO_ACTUAL, TODO, PERSONALIZADO }`:
  - Convierte el alcance en (fechaDesde, fechaHasta) con `java.time.LocalDate`.
  - Recupera gastos e ingresos, calcula `ingresoTotal`.
  - Devuelve además la etiqueta de la hoja y el nombre de archivo sugerido.

### Generador xlsx (commonMain, sin dependencias)

- `XlsxGenerador.generar(contenido): ByteArray`:
  - Escritor ZIP mínimo propio (entradas `STORED` + CRC32) — no se usa `java.util.zip` porque no está
    disponible en `commonMain`; ambas plataformas (Android y JVM) consumen el mismo `ByteArray`.
  - Archivos OOXML generados:
    - `[Content_Types].xml`, `_rels/.rels`, `xl/workbook.xml`, `xl/_rels/workbook.xml.rels`,
      `xl/worksheets/sheet1.xml`, `xl/sharedStrings.xml`, `xl/styles.xml`.
  - Descripciones/categorías se escriben vía `sharedStrings.xml` (índices) igual que en el ejemplo.
- Hoja `sheet1.xml` — réplica del ejemplo:
  - Fila 1 (encabezados centrados): `A Monto, B Descripcion, C Categoria, D Fecha, E Cuota`,
    `G Saldo del mes`, `I Balance disponible`. Columnas `F` y `H` vacías de separación.
  - Filas de gastos (una por gasto): `A` monto, `B` descripción, `C` categoría,
    `D` fecha como **número serial Excel** + `numFmt` `dd/mm/yyyy`, `E` cuota.
  - Columna `E` "Cuota": si `gasto.esCuota` → texto `"Cuota X/Y"` (`cuotaActual/cuotaTotal`);
    si no → celda vacía. (El ejemplo mostró una fecha `d/m` que no es reproducible con el modelo
    actual; decisión confirmada por el usuario.)
  - En la primera fila de datos: `G2 = ingresoTotal`; `I2 = =G2-SUM(A2:A{n+1})`;
    `I3 = =FIXED((I2/G2)*100.2)&"%"` (idénticas al ejemplo).
  - Autofiltro `A1:E{n}` sobre la tabla de gastos.
  - Hoja nombrada según alcance: Mes → `"Sep 2026"`, Año → `"2026"`, Todo → `"Gastos"`,
    Personalizado → `"Gastos"`.

### Guardado (expect/actual composable)

Nuevo puente composable en `commonMain`, patrón idéntico al de `SettingsStore`:

```kotlin
@Composable
expect fun GuardarXlsxHost(
    nombreSugerido: String,
    bytes: ByteArray?,      // null → inactivo; no-null → dispara el guardado una sola vez
    onResult: (Boolean) -> Unit
)
```

- El `ExportarDialog` de Ajustes incluye siempre este host; cuando los bytes están listos, la
  plataforma muestra el selector y devuelve `onResult(true)` al guardar o `onResult(false)` al cancelar.
- `jvmMain` actual: `javax.swing.JFileChooser` (modo guardar) con nombre sugerido; si elige,
  `File.writeBytes(bytes)`.
- `androidMain` actual: `rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(nombreSugerido))`
  → URI → `contentResolver.openOutputStream(uri).use { it.write(bytes) }` (launcher registrado en composición).

### UI (Ajustes)

- `AjustesScreen`: la fila "Exportar" abre un `ExportarDialog` (composable nuevo en `AjustesScreen.kt`
  o archivo propio).
- El diálogo contiene:
  - Radio con los 4 alcances (por defecto "Mes actual").
  - Si "Personalizado": dos selectores de fecha desde/hasta (reutilizar `DatePicker` de M3 que ya usa
    la app). Validación `desde <= hasta`.
  - Botón "Exportar" (deshabilitado si el rango personalizado es inválido).
- Flujo: confirma → `ExportarViewModel.exportar(...)` (corutina en `viewModelScope`) → manda el
  `ExportarContenido` al generador → `guardarXlsx` → Snackbar de éxito/error (siguiendo el patrón
  `mensajeExito`/`mensajeError` de los ViewModels existentes).
- `ExportarViewModel` (factory en `AppModule`) recibe `ExportarGastosUseCase` por constructor.

### i18n

Claves nuevas `exportar.*` en `EsStrings`/`EnStrings`/`FrStrings` (patrón ternario existente):

- `exportar.titulo` (título del diálogo)
- `exportar.mesActual`, `exportar.anioActual`, `exportar.todo`, `exportar.personalizado`
- `exportar.desde`, `exportar.hasta`
- `exportar.boton`, `exportar.exito`, `exportar.error`, `exportar.sinDatos`, `exportar.confirmar`

## Errores y casos límite

- Rango personalizado inválido (`desde > hasta`): botón deshabilitado + herramienta de validación.
- Sin gastos en el rango: Snackbar de aviso "sin datos" antes de abrir el guardado (no se genera archivo vacío sin confirmación).
- Guardado cancelado (usuario cierra el diálogo de guardar): no se muestra error, simplemente no se guarda.
- `ingresoTotal == 0` (no hay ingresos definidos): se omite la fila del porcentaje (`I3`) y las
  celdas `G`/`I`; solo se exporta la tabla de gastos (evita `#DIV/0!`).
- Nombres de archivo: caracteres no válidos para nombres de archivo se remplazan/sanean antes de sugerir.

## Verification

- Compilación: `.\gradlew.bat :composeApp:compileKotlinJvm :composeApp:compileDebugKotlinAndroid`.
- Prueba manual Desktop: Ajustes → Exportar → cada alcance → archivo se abre en Excel/WPS y muestra
  columnas, fechas `dd/mm/yyyy`, fórmulas y balance correctos.
- Prueba manual Android: guardado vía SAF en cada alcance.
- El `XlsxGenerador` (función pura) es candidato a test unitario si el proyecto tiene infraestructura
  de test configurada; si no, se verifica por apertura manual del archivo.

## Fuera de alcance (iteraciones futuras)

- Gráfico circular embebido (como en `Ejemplo.xlsx`).
- Importación desde archivo.
- Persistencia de la última opción de exportación elegida.