# Diseño: Exportar a .xlsx — hojas mensuales + fix calendario personalizado

Fecha: 2026-09-11
Estado: Aprobado por el usuario (2026-09-11)
Extiende: `docs/superpowers/specs/2026-09-10-exportar-xlsx-design.md` (feature exportar.xlsx original, 2026-09-10)

## Objetivo

Dos cambios sobre la feature "Exportar a .xlsx" ya terminada (9/9 tareas + review final READY TO SHIP):

1. **Fix UI**: en el alcance "Personalizado" del diálogo de exportación, los campos Desde/Hasta se
   muestran pero el calendario (`DatePickerDialog`) no se abre al hacer clic.
2. **Extensión de comportamiento**: los cuatro alcances deben separar los gastos **por mes** en
   hojas distintas dentro de un mismo archivo `.xlsx` (Año actual → 12 hojas como "Ene 2026",
   "Feb 2026", …; Todo y Personalizado → una hoja por mes con datos).

## Causa raíz (fix del calendario)

`FechaCampo` en `ExportarDialog.kt:164` envuelve un `OutlinedTextField(readOnly = true)` en un
`Box(Modifier.clickable(onClick = onClick))`. El text field en modo `readOnly` **consume el evento de
clic** (pide foco / posiciona el cursor), por lo que el `clickable` del `Box` (padre, dibujado debajo)
nunca se dispara → `editarCampo` permanece `null` → `DatePickerDialog` no se muestra.

El patrón que sí funciona en la app (RegistrarGastoScreen.kt:204-225 y EditarGastoScreen.kt:153-174)
es un `Surface(onClick = { … })` sin text field: `Surface` + `Row` con `Column(etiqueta, valor)` y el
ícono del calendario.

## Alcance de la separación por mes (decisiones del usuario)

- **Los cuatro alcances** separan por mes: Mes actual (1 mes → 1 hoja, sin cambio visible),
  Año actual (hojas por mes del año), Todo (hoja por mes con datos), Personalizado (hoja por mes con
  datos dentro del rango).
- **Se omiten las hojas de meses sin gastos** (no se generan hojas vacías).
- **El ingreso de cada hoja es el de ese mes** (`IngresoMensual` de ese año/mes); si el mes no tiene
  ingreso registrado → `ingresoTotal = 0` y se omiten las celdas `G2`/`I2`/`I3` (comportamiento
  ya implementado y testeado).
- Nombres de hoja: `"${Strings.mes(mes)} $anio"` (ej. "Ene 2026"), igual que la convención actual
  de `hojaDe` para Mes actual. Al los meses repetirse entre años en Todo/Personalizado, el año
  desambigua.
- Nombre de archivo sugerido: **sin cambios** respecto al comportamiento actual (por alcance).

## Arquitectura

### Modelo (`domain/model/Exportacion.kt`)

```kotlin
data class HojaExcel(
    val nombre: String,          // "Ene 2026"
    val gastos: List<Gasto>,
    val ingresoTotal: Double     // ingreso mensual de ese (anio, mes); 0 si no hay registro
)

data class ExportarContenido(
    val nombreArchivo: String,
    val hojas: List<HojaExcel>   // elimina hoja/…/gastos/ingresoTotal únicos actuales
)

data class ExportarDatos(
    val gastos: List<Gasto>,
    val ingresos: List<IngresoMensual>,   // se agrega: registros del rango consultado
    val desde: LocalDate,
    val hasta: LocalDate,
    val alcance: ExportarAlcance
)
```

`ExportarGastosUseCase` (sin cambios de queries): reemplaza el `ingresoTotal` sumado por
`ingresos = ingresoMensualRepository.obtenerEnRango(desdeKey, hastaKey)`.

### ViewModel (`ExportarViewModel`)

`exportar()` agrupa `datos.gastos` (ya ordenados ASC por `fecha`) por `(anio, mes)` preservando el
orden cronológico de los meses; por cada grupo crea:

```kotlin
HojaExcel(
    nombre = "${Strings.mes(mes)} $anio",
    gastos = gastosDelMes,
    ingresoTotal = datos.ingresos
        .firstOrNull { it.anio == anio && it.mes == mes }?.monto ?: 0.0
)
```

Si `datos.gastos` está vacío se conserva el mensaje `exportar.sinDatos` (sin cambios). Luego
`XlsxGenerador.generar(contenido)` y el resto del flujo no cambia (guardado, mensajes, snackbar).

### Generador (`XlsxGenerador`)

`generar(contenido: ExportarContenido): ByteArray` itera `contenido.hojas`:

- Un `sheetN.xml` por hoja (mismo XML de hoja actual, con `saldo/balance/porcentaje` por mes).
- `xl/workbook.xml` con un `<sheet name="…" sheetId="N" r:id="rIdN"/>` por hoja.
- `[Content_Types].xml` con un `<Override PartName="/xl/worksheets/sheetN.xml" …/>` por hoja.
- `xl/_rels/workbook.xml.rels` con una relación `rIdN → worksheets/sheetN.xml` por hoja,
  + `sharedStrings` + `styles` (rIds siguientes).
- `xl/sharedStrings.xml` **único** compartido por todas las hojas (indices globales, patrón actual).
- El resto de archivos OOXML se mantiene.

### Fix UI (`ExportarDialog.kt`)

`FechaCampo` pasa a usar el patrón existente sin text field:

```kotlin
Surface(
    onClick = onClick,
    shape   = RoundedCornerShape(4.dp),
    color   = AppTheme.CardDark,
    border  = BorderStroke(1.dp, AppTheme.BorderColor),
    modifier = Modifier.fillMaxWidth()
) {
    Row(/* padding 16/12, SpaceBetween, CenterVertically */) {
        Column {
            Text(etiqueta, color = AppTheme.TextMuted, fontSize = 12.sp)
            Text(valor?.toString() ?: tr("registrar.seleccionarFecha"), …)  // clave existente
        }
        Icon(AppIcons.Calendario, …)
    }
}
```

Adaptado del patrón Registrar/Editar para caber en la fila Desde/Hasta del diálogo (sigue siendo
vertical: etiqueta arriba, superficie abajo). Imports nuevos: `Surface`, `BorderStroke`,
`RoundedCornerShape`, `AppIcons.Calendario`.

## Errores y casos límite (sin cambios adicionales)

- `ingresoTotal == 0` por mes → omitir `G2`/`I2`/`I3` (ya implementado).
- Meses sin gastos → no se crea hoja.
- Sin gastos en el alcance → `exportar.sinDatos` (ya implementado).
- Orden: los gastos se agrupan en orden ascendente por mes (fecha girando; `groupBy` con
  `LinkedHashMap` o sort posterior de claves para preservar cronología).

## Verification

- Compilación: `.\gradlew.bat :composeApp:compileKotlinJvm :composeApp:compileDebugKotlinAndroid`
  y `.\gradlew.bat :composeApp:jvmTest`.
- `XlsxGeneradorTest`: se amplía/ajusta a la nueva firma; casos a cubrir:
  - libro multi-hoja: `workbook.xml` con N `<sheet name=…>` y N `sheetN.xml` presentes en el zip;
  - una hoja por mes con gastos, nombres "Ene 2026"… ;
  - meses sin gastos omitidos (no están en `workbook.xml`);
  - `sharedStrings` compartido (indices de la hoja 2 resuelven textos de la hoja 1);
  - G/I por mes: un mes con ingreso y otro con 0 → solo el primero lleva `G2`/`I2`.
- Prueba manual Desktop: Exportar en los 4 alcances → Excel/WPS abre un libro con una hoja por mes.

## Fuera de alcance (se mantiene del spec original)

- Gráfico circular embebido.
- Importación desde archivo.
- Persistencia de la última opción de exportación.