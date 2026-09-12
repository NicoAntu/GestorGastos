# Exportar a .xlsx — Plan de Implementación

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Generar archivos `.xlsx` reales (OOXML) con los gastos desde la pantalla Ajustes, en 4 alcances (Mes actual, Año actual, Todo, Personalizado), réplica del ejemplo del usuario.

**Architecture:** Cadena de capas siguiendo el patrón del proyecto: SQLDelight (2 queries nuevas) → repositorios → `ExportarGastosUseCase` → `ExportarViewModel` → diálogo en Ajustes. Un generador xlsx puro en `commonMain` (ZIP mínimo + XML OOXML, sin dependencias nuevas) produce el `ByteArray`; el guardado se delega en un `expect class ExportarGuardado` (`JFileChooser` en Desktop, `CreateDocument`/SAF en Android).

**Tech Stack:** Kotlin Multiplatform (composeApp), Compose Multiplatform, SQLDelight, Koin, `java.time` (ya usado en commonMain), `kotlin.test` (commonTest).

## Global Constraints

- **No es repositorio git** (`D:\GestorGastos` no es repo). **Se omiten los pasos de commit.** Cada tarea termina en su gate de compilación/pruebas.
- Compilación: `.\gradlew.bat :composeApp:compileKotlinJvm :composeApp:compileDebugKotlinAndroid`.
- Pruebas: `.\gradlew.bat :composeApp:jvmTest`.
- **No agregar dependencias nuevas.**
- `java.time` está disponible en `commonMain` (ya lo usan `FechaUtils.kt`, `EditarGastoScreen.kt`, `HomeViewModel.kt`).
- Fechas de gastos: String `YYYY-MM-DD` (orden lexicográfico = cronológico).
- i18n ternario ES/EN/FR con claves nuevas `exportar.*`; textos de UI vía `tr(...)` (composable) o `Strings.t(...)` (no composable). Meses abreviados vía `Strings.mes(indice)`.
- UI: usar `AppTheme.*` y componentes Material3 como el resto de pantallas.
- `ViewModel` de Koin: pattern `factory` en `viewModelModule`; platforms beans en `desktopModule`/`androidModule`.

## File Structure

**Nuevos archivos (commonMain):**
- `com/angel/gg/domain/model/Exportacion.kt` — `enum ExportarAlcance`, `data class ExportarDatos`, `data class ExportarContenido`
- `com/angel/gg/export/ZipWriter.kt` — ZIP mínimo (STORED + CRC32 propio)
- `com/angel/gg/export/XlsxGenerador.kt` — genera `ByteArray` xlsx
- `com/angel/gg/export/ExportarGuardado.kt` — `expect class ExportarGuardado`
- `com/angel/gg/domain/usecase/ExportarGastosUseCase.kt`
- `com/angel/gg/presentation/viewmodel/ExportarViewModel.kt`
- `com/angel/gg/presentation/screen/ExportarDialog.kt`

**Nuevos archivos (plataformas):**
- `jvmMain/.../export/ExportarGuardado.jvm.kt`
- `androidMain/.../export/ExportarGuardado.android.kt` (+ object `ExportarPuente`)

**Nuevos archivos (tests):**
- `commonTest/.../export/XlsxGeneradorTest.kt`

**Modificados:**
- `sqldelight/.../Gasto.sq`, `sqldelight/.../IngresoMensual.sq`
- `domain/repository/GastoRepository.kt`, `data/repository/GastoRepositoryImpl.kt`
- `domain/repository/IngresoMensualRepository.kt`, `data/repository/IngresoMensualRepositoryImpl.kt`
- `di/AppModule.kt`, `di/DesktopModule.kt`, `di/AndroidModule.kt`
- `androidMain/.../MainActivity.kt`
- `presentation/screen/AjustesScreen.kt`
- `presentation/i18n/EsStrings.kt`, `EnStrings.kt`, `FrStrings.kt`

---

### Task 1: ZipWriter (commonMain, puro Kotlin) + test

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/angel/gg/export/ZipWriter.kt`
- Test: `composeApp/src/commonTest/kotlin/com/angel/gg/export/XlsxGeneradorTest.kt`

**Interfaces:**
- Produces: `object ZipWriter { fun zip(entries: List<Pair<String, ByteArray>>): ByteArray }` — entradas STORED, flag UTF-8, CRC32 propio.

- [ ] **Step 1: Escribir el test que falla**

```kotlin
package com.angel.gg.export

import kotlin.test.Test
import kotlin.test.assertTrue

class XlsxGeneradorTest {

    private fun ByteArray.aTexto(): String =
        buildString { forEach { append((it.toInt() and 0xFF).toChar()) } }

    @Test
    fun zipGeneradoTieneFirmasYNombre() {
        val bytes = ZipWriter.zip(listOf("hola.txt" to "mundo".toByteArray()))
        val texto = bytes.aTexto()
        assertTrue(texto.startsWith("PK\u0003\u0004"), "falta firma local header")
        assertTrue(texto.contains("PK\u0001\u0002"), "falta firma central directory")
        assertTrue(texto.contains("PK\u0005\u0006"), "falta firma EOCD")
        assertTrue(texto.contains("hola.txt"), "falta el nombre de la entrada")
        assertTrue(texto.contains("mundo"), "falta el contenido")
    }
}
```

- [ ] **Step 2: Ejecutar el test y verificar que falla**

Run: `.\gradlew.bat :composeApp:jvmTest`
Expected: FAIL — `ZipWriter` no existe (compilation error).

- [ ] **Step 3: Implementar ZipWriter**

```kotlin
package com.angel.gg.export

object ZipWriter {

    private const val SIG_LOCAL   = 0x04034b50L
    private const val SIG_CENTRAL = 0x02014b50L
    private const val SIG_EOCD    = 0x06054b50L
    private const val FLAG_UTF8   = 0x0800

    fun zip(entries: List<Pair<String, ByteArray>>): ByteArray {
        val piezas = ArrayList<ByteArray>()
        fun push(b: ByteArray) { piezas.add(b) }

        val central = ArrayList<ByteArray>()
        var offset = 0L

        entries.forEach { (nombre, data) ->
            val crc = crc32(data)
            val nombreBytes = nombre.toByteArray(Charsets.UTF_8)

            val local = BuilderPiezas()
            local.u32(SIG_LOCAL)
            local.u16(20)            // version needed
            local.u16(FLAG_UTF8)
            local.u16(0)             // método STORED
            local.u16(0)
            local.u16(0x21)          // fecha 1980-01-01
            local.u32(crc)
            local.u32(data.size.toLong())
            local.u32(data.size.toLong())
            local.u16(nombreBytes.size)
            local.u16(0)             // extra
            local.bytes(nombreBytes)
            push(local.build())
            push(data)

            val c = BuilderPiezas()
            c.u32(SIG_CENTRAL)
            c.u16(20)
            c.u16(20)
            c.u16(FLAG_UTF8)
            c.u16(0)
            c.u16(0)
            c.u16(0x21)
            c.u32(crc)
            c.u32(data.size.toLong())
            c.u32(data.size.toLong())
            c.u16(nombreBytes.size)
            c.u16(0)
            c.u16(0)
            c.u16(0)
            c.u16(0)
            c.u32(0)
            c.u32(offset)
            c.bytes(nombreBytes)
            central.add(c.build())

            offset += local.size() + data.size
        }

        val centralSize = central.sumOf { it.size.toLong() }
        val centralPieza = BuilderPiezas()
        central.forEach { centralPieza.bytes(it) }
        push(centralPieza.build())

        val eocd = BuilderPiezas()
        eocd.u32(SIG_EOCD)
        eocd.u16(0)
        eocd.u16(0)
        eocd.u16(entries.size)
        eocd.u16(entries.size)
        eocd.u32(centralSize)
        eocd.u32(offset)
        eocd.u16(0)
        push(eocd.build())

        val total = piezas.sumOf { it.size.toLong() }.toInt()
        val out = ByteArray(total)
        var pos = 0
        piezas.forEach { it.copyInto(out, pos); pos += it.size }
        return out
    }

    private val TABLA_CRC = IntArray(256).also { tabla ->
        for (n in 0..255) {
            var c = n
            repeat(8) {
                c = if (c and 1 != 0) (0xEDB88320.toInt() xor (c ushr 1)) else c ushr 1
            }
            tabla[n] = c
        }
    }

    fun crc32(data: ByteArray): Long {
        var crc = 0xFFFFFFFFL.toInt()
        data.forEach { b -> crc = TABLA_CRC[(crc xor b.toInt()) and 0xFF] xor (crc ushr 8) }
        return (crc xor -1).toLong() and 0xFFFFFFFFL
    }
}

internal class BuilderPiezas {
    private val piezas = ArrayList<ByteArray>()
    private var tamaño = 0

    fun u16(v: Int) { piezas.add(byteArrayOf((v and 0xFF).toByte(), ((v ushr 8) and 0xFF).toByte())); tamaño += 2 }
    fun u32(v: Long) {
        piezas.add(byteArrayOf(
            (v and 0xFF).toByte(), ((v ushr 8) and 0xFF).toByte(),
            ((v ushr 16) and 0xFF).toByte(), ((v ushr 24) and 0xFF).toByte()
        ))
        tamaño += 4
    }
    fun bytes(b: ByteArray) { piezas.add(b); tamaño += b.size }
    fun size(): Int = tamaño
    fun build(): ByteArray {
        val out = ByteArray(tamaño)
        var pos = 0
        piezas.forEach { it.copyInto(out, pos); pos += it.size }
        return out
    }
}
```

- [ ] **Step 4: Ejecutar el test y verificar que pasa**

Run: `.\gradlew.bat :composeApp:jvmTest`
Expected: PASS (2 asserts sin invalid). No hay gate de Android todavía (ZipWriter está en commonMain; se validará con la Task 3).

- [ ] **Step 5: Gate de compilación**

Run: `.\gradlew.bat :composeApp:compileKotlinJvm`
Expected: BUILD SUCCESSFUL.

---

### Task 2: Modelos + XlsxGenerador (commonMain) + tests

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/angel/gg/domain/model/Exportacion.kt`
- Create: `composeApp/src/commonMain/kotlin/com/angel/gg/export/XlsxGenerador.kt`
- Test: `composeApp/src/commonTest/kotlin/com/angel/gg/export/XlsxGeneradorTest.kt`

**Interfaces:**
- Consumes: `ZipWriter.zip(List<Pair<String, ByteArray>>)` (Task 1); `Gasto` (`com.angel.gg.domain.model.Gasto`).
- Produces:
  - `enum ExportarAlcance { MES_ACTUAL, ANIO_ACTUAL, TODO, PERSONALIZADO }`
  - `data class ExportarDatos(val gastos: List<Gasto>, val ingresoTotal: Double, val desde: LocalDate, val hasta: LocalDate, val alcance: ExportarAlcance)`
  - `data class ExportarContenido(val hoja: String, val nombreArchivo: String, val gastos: List<Gasto>, val ingresoTotal: Double)`
  - `object XlsxGenerador { fun generar(contenido: ExportarContenido): ByteArray; fun serialFecha(fechaIso: String): String; fun formatearNumero(v: Double): String }`

- [ ] **Step 1: Escribir los tests que fallan**

```kotlin
package com.angel.gg.export

import com.angel.gg.domain.model.ExportarContenido
import com.angel.gg.domain.model.Gasto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import java.time.LocalDate

class XlsxGeneradorTest {

    private fun ByteArray.aTexto(): String =
        buildString { forEach { append((it.toInt() and 0xFF).toChar()) } }

    private val gastos = listOf(
        Gasto(
            id = "1", monto = 100.0, descripcion = "Pago mensual",
            categoriaId = "c1", categoriaNombre = "Luz", categoriaColor = "#4285F4",
            categoriaIcono = "", fecha = "2026-09-01", anio = 2026, mes = 9
        ),
        Gasto(
            id = "2", monto = 222.0, descripcion = "Pago mensual",
            categoriaId = "c2", categoriaNombre = "Departamento", categoriaColor = "#EA4335",
            categoriaIcono = "", fecha = "2026-09-02", anio = 2026, mes = 9,
            cuotaActual = 1, cuotaTotal = 6, idPadre = "padre-1"
        )
    )

    private val contenido = ExportarContenido(
        hoja = "Sep 2026", nombreArchivo = "Gastos Sep 2026.xlsx",
        gastos = gastos, ingresoTotal = 3565.0
    )

    @Test
    fun serialFechaConvierteAExcel() {
        val esperado = (LocalDate.of(2026, 9, 1).toEpochDay() - LocalDate.of(1899, 12, 30).toEpochDay()).toString()
        assertEquals(esperado, XlsxGenerador.serialFecha("2026-09-01"))
    }

    @Test
    fun formatearNumeroTruncaDecimales() {
        assertEquals("100", XlsxGenerador.formatearNumero(100.0))
        assertEquals("100.5", XlsxGenerador.formatearNumero(100.5))
    }

    @Test
    fun generadorProduceHojaEsperada() {
        val bytes = XlsxGenerador.generar(contenido)
        val s = bytes.aTexto()

        assertTrue(s.startsWith("PK\u0003\u0004"), "debe ser un zip")
        assertTrue(s.contains("<sheet name=\"Sep 2026\""), "faltó el nombre de hoja")
        assertTrue(s.contains("Saldo del mes"), "faltó encabezado G")
        assertTrue(s.contains("Balance disponible"), "faltó encabezado I")
        assertTrue(s.contains("G2-SUM(A:A)"), "faltó fórmula de balance")
        assertTrue(s.contains("FIXED((I2/G2)*100.2)&amp;\"%\""), "faltó fórmula de porcentaje")
        assertTrue(s.contains("Cuota 1/6"), "faltó la cuota del gasto con idPadre")
        assertTrue(s.contains("46266"), "faltó la fecha serial")
        assertTrue(s.contains("<autoFilter ref=\"A1:E3\"/>"), "faltó el autofiltro")
    }
}
```

- [ ] **Step 2: Ejecutar los tests y verificar que fallan**

Run: `.\gradlew.bat :composeApp:jvmTest`
Expected: FAIL — `ExportarAlcance`, `ExportarContenido`, `ExportarDatos`, `XlsxGenerador` no existen.

- [ ] **Step 3: Crear los modelos**

```kotlin
package com.angel.gg.domain.model

import java.time.LocalDate

enum class ExportarAlcance { MES_ACTUAL, ANIO_ACTUAL, TODO, PERSONALIZADO }

data class ExportarDatos(
    val gastos: List<Gasto>,
    val ingresoTotal: Double,
    val desde: LocalDate,
    val hasta: LocalDate,
    val alcance: ExportarAlcance
)

data class ExportarContenido(
    val hoja: String,
    val nombreArchivo: String,
    val gastos: List<Gasto>,
    val ingresoTotal: Double
)
```

- [ ] **Step 4: Implementar XlsxGenerador**

```kotlin
package com.angel.gg.export

import com.angel.gg.domain.model.ExportarContenido
import java.time.LocalDate

object XlsxGenerador {

    private val BASE_EXCEL = LocalDate.of(1899, 12, 30).toEpochDay()

    fun serialFecha(fechaIso: String): String =
        (LocalDate.parse(fechaIso).toEpochDay() - BASE_EXCEL).toString()

    fun formatearNumero(v: Double): String =
        if (v == kotlin.math.floor(v)) v.toLong().toString() else v.toString()

    fun generar(contenido: ExportarContenido): ByteArray {
        val textos = ArrayList<String>()
        fun idx(t: String): Int = textos.indexOf(t).let {
            if (it != -1) it else { textos.add(t); textos.size - 1 }
        }
        fun xml(t: String): String = t
            .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

        listOf("Monto", "Descripcion", "Categoria", "Fecha", "Cuota",
               "Saldo del mes", "Balance disponible").forEach { idx(it) }

        val n = contenido.gastos.size
        val ultimaFila = n + 1

        val sheet = StringBuilder()
        sheet.append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">")
        sheet.append("<cols>" +
            "<col min=\"1\" max=\"1\" width=\"12\" customWidth=\"1\"/>" +
            "<col min=\"2\" max=\"2\" width=\"20.25\" customWidth=\"1\"/>" +
            "<col min=\"3\" max=\"3\" width=\"18\" customWidth=\"1\"/>" +
            "<col min=\"4\" max=\"4\" width=\"12\" customWidth=\"1\"/>" +
            "<col min=\"5\" max=\"5\" width=\"12\" customWidth=\"1\"/>" +
            "<col min=\"7\" max=\"7\" width=\"14\" customWidth=\"1\"/>" +
            "<col min=\"9\" max=\"9\" width=\"18\" customWidth=\"1\"/></cols>")
        sheet.append("<sheetData>")

        // Fila 1: encabezados
        sheet.append("<row r=\"1\">")
        listOf("A", "B", "C", "D", "E").forEachIndexed { i, col ->
            sheet.append("<c r=\"${col}1\" s=\"1\" t=\"s\"><v>${idx(headerDe(i))}</v></c>")
        }
        sheet.append("<c r=\"F1\" s=\"2\"/>")
        sheet.append("<c r=\"G1\" s=\"1\" t=\"s\"><v>${idx("Saldo del mes")}</v></c>")
        sheet.append("<c r=\"H1\" s=\"2\"/>")
        sheet.append("<c r=\"I1\" s=\"1\" t=\"s\"><v>${idx("Balance disponible")}</v></c>")
        sheet.append("</row>")

        // Filas de datos (r = 2..n+1). G2/I2 en la primera fila de datos.
        contenido.gastos.forEachIndexed { i, gasto ->
            val r = i + 2
            sheet.append("<row r=\"$r\">")
            sheet.append("<c r=\"A$r\" s=\"1\" t=\"n\"><v>${formatearNumero(gasto.monto)}</v></c>")
            sheet.append("<c r=\"B$r\" s=\"1\" t=\"s\"><v>${idx(gasto.descripcion)}</v></c>")
            sheet.append("<c r=\"C$r\" s=\"3\" t=\"s\"><v>${idx(gasto.categoriaNombre)}</v></c>")
            sheet.append("<c r=\"D$r\" s=\"4\" t=\"n\"><v>${serialFecha(gasto.fecha)}</v></c>")
            if (gasto.esCuota && gasto.cuotaActual != null && gasto.cuotaTotal != null) {
                sheet.append("<c r=\"E$r\" s=\"3\" t=\"s\"><v>${idx("Cuota ${gasto.cuotaActual}/${gasto.cuotaTotal}")}</v></c>")
            } else {
                sheet.append("<c r=\"E$r\" s=\"2\"/>")
            }
            sheet.append("<c r=\"F$r\" s=\"2\"/>")
            if (r == 2) {
                sheet.append("<c r=\"G2\" s=\"1\" t=\"n\"><v>${formatearNumero(contenido.ingresoTotal)}</v></c>")
            }
            sheet.append("<c r=\"H$r\" s=\"2\"/>")
            if (r == 2) {
                sheet.append("<c r=\"I2\" s=\"1\"><f>G2-SUM(A:A)</f><v>${formatearNumero(balanceDe(contenido))}</v></c>")
            }
            sheet.append("</row>")
        }

        // Porcentaje en I3 (fila 3), igual que el ejemplo. Solo si hay ingreso.
        if (contenido.ingresoTotal > 0) {
            sheet.append("<row r=\"3\"><c r=\"I3\" s=\"1\" t=\"str\"><f>FIXED((I2/G2)*100.2)&amp;\"%\"</f><v>${porcentajeDe(contenido)}</v></c></row>")
        }

        sheet.append("</sheetData>")
        sheet.append("<autoFilter ref=\"A1:E$ultimaFila\"/>")
        sheet.append("</worksheet>")

        return ZipWriter.zip(listOf(
            "[Content_Types].xml" to contentTypesXml().toByteArray(),
            "_rels/.rels"          to rootRelsXml().toByteArray(),
            "xl/workbook.xml"      to workbookXml(contenido.hoja).toByteArray(),
            "xl/_rels/workbook.xml.rels" to workbookRelsXml().toByteArray(),
            "xl/worksheets/sheet1.xml"   to sheet.toString().toByteArray(),
            "xl/sharedStrings.xml"       to sharedStringsXml(textos).toByteArray(),
            "xl/styles.xml"              to stylesXml().toByteArray()
        ))
    }

    private fun headerDe(indice: Int): String =
        listOf("Monto", "Descripcion", "Categoria", "Fecha", "Cuota")[indice]

    private fun balanceDe(c: ExportarContenido): Double =
        c.ingresoTotal - c.gastos.sumOf { it.monto }

    private fun porcentajeDe(c: ExportarContenido): String {
        val b = balanceDe(c)
        if (c.ingresoTotal <= 0) return "0,00%"
        val valor = (b / c.ingresoTotal) * 100.2
        return (String.format("%.2f", valor).replace('.', ',')) + "%"
    }

    private fun contentTypesXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
<Default Extension="xml" ContentType="application/xml"/>
<Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
<Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
<Override PartName="/xl/sharedStrings.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sharedStrings+xml"/>
<Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
</Types>"""

    private fun rootRelsXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""

    private fun workbookXml(hoja: String): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"
 xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
<sheets><sheet name="$hoja" sheetId="1" r:id="rId1"/></sheets>
</workbook>"""

    private fun workbookRelsXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
<Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/sharedStrings" Target="sharedStrings.xml"/>
<Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>"""

    private fun sharedStringsXml(textos: List<String>): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<sst xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" count="${textos.size}" uniqueCount="${textos.size}">""")
        textos.forEach { sb.append("<si><t>${it.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")}</t></si>") }
        sb.append("</sst>")
        return sb.toString()
    }

    private fun stylesXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
<numFmts count="1"><numFmt numFmtId="164" formatCode="dd/mm/yyyy"/></numFmts>
<fonts count="2">
<font><sz val="10.0"/><color rgb="FF000000"/><name val="Arial"/><scheme val="minor"/></font>
<font><b/><color theme="1"/><name val="Arial"/><scheme val="minor"/></font>
</fonts>
<fills count="2">
<fill><patternFill patternType="none"/></fill>
<fill><patternFill patternType="lightGray"/></fill>
</fills>
<borders count="1"><border/></borders>
<cellStyleXfs count="1"><xf borderId="0" fillId="0" fontId="0" numFmtId="0" xfId="0"/></cellStyleXfs>
<cellXfs count="5">
<xf borderId="0" fillId="0" fontId="0" numFmtId="0" xfId="0"/>
<xf borderId="0" fillId="0" fontId="1" numFmtId="0" xfId="0" applyFont="1"><alignment horizontal="center"/></xf>
<xf borderId="0" fillId="1" fontId="0" numFmtId="0" xfId="0" applyFill="1"><alignment horizontal="center"/></xf>
<xf borderId="0" fillId="0" fontId="0" numFmtId="0" xfId="0"><alignment horizontal="left"/></xf>
<xf borderId="0" fillId="0" fontId="0" numFmtId="164" xfId="0" applyNumberFormat="1"><alignment horizontal="center"/></xf>
</cellXfs>
</styleSheet>"""
}
```

> Nota: usa `String.format` (JVM y Android lo soportan en commonMain compilando contra la JDK; el proyecto ya usa APIs de JDK en commonMain). Si `String.format` diera problemas para Android, sustituir por `kotlin.math.round(valor * 100) / 100.0` y concatenar.

- [ ] **Step 5: Ejecutar los tests y verificar que pasan**

Run: `.\gradlew.bat :composeApp:jvmTest`
Expected: PASS de los 3 tests (si `String.format` falla en common, ajustar según nota).

- [ ] **Step 6: Gate de compilación**

Run: `.\gradlew.bat :composeApp:compileKotlinJvm :composeApp:compileDebugKotlinAndroid`
Expected: BUILD SUCCESSFUL.

---

### Task 3: Queries SQLDelight + métodos de repositorio

**Files:**
- Modify: `composeApp/src/commonMain/sqldelight/com/angel/gg/db/Gasto.sq`
- Modify: `composeApp/src/commonMain/sqldelight/com/angel/gg/db/IngresoMensual.sq`
- Modify: `composeApp/src/commonMain/kotlin/com/angel/gg/domain/repository/GastoRepository.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/angel/gg/data/repository/GastoRepositoryImpl.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/angel/gg/domain/repository/IngresoMensualRepository.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/angel/gg/data/repository/IngresoMensualRepositoryImpl.kt`

**Interfaces:**
- Producer (useCase, Task 4): `GastoRepository.obtenerPorRango(desde: String, hasta: String): List<Gasto>` y `IngresoMensualRepository.obtenerEnRango(desdeKey: Long, hastaKey: Long): List<IngresoMensual>` (ambos `suspend`).

- [ ] **Step 1: Agregar la query `obtenerPorRango` en Gasto.sq**

Añadir al final de `Gasto.sq`:

```sql
-- Gastos en un rango de fechas (para exportación)
obtenerPorRango:
SELECT
    g.*,
    c.nombre      AS categoria_nombre,
    c.color       AS categoria_color,
    c.icono       AS categoria_icono
FROM Gasto g
INNER JOIN Categoria c ON g.categoria_id = c.id
WHERE g.fecha >= ? AND g.fecha <= ?
ORDER BY g.fecha ASC, g.rowid ASC;
```

- [ ] **Step 2: Agregar la query `obtenerEnRango` en IngresoMensual.sq**

Añadir al final de `IngresoMensual.sq`:

```sql
-- Ingresos por rango de meses (para exportación)
obtenerEnRango:
SELECT * FROM IngresoMensual
WHERE (anio * 100 + mes) >= ? AND (anio * 100 + mes) <= ?;
```

- [ ] **Step 3: Ampliar la interfaz GastoRepository**

En `GastoRepository.kt`, agregar:

```kotlin
    // Rango de fechas (exportación)
    suspend fun obtenerPorRango(desde: String, hasta: String): List<Gasto>
```

- [ ] **Step 4: Implementar en GastoRepositoryImpl**

Agregar el override y el mapper (copia del mapper `ObtenerPorMes`):

```kotlin
    override suspend fun obtenerPorRango(desde: String, hasta: String): List<Gasto> =
        withContext(Dispatchers.Default) {
            queries.obtenerPorRango(desde = desde, hasta = hasta)
                .executeAsList()
                .map { it.toDomain() }
        }
```

```kotlin
    private fun com.angel.gg.db.ObtenerPorRango.toDomain() = Gasto(
        id              = id,
        monto           = monto,
        descripcion     = descripcion,
        categoriaId     = categoria_id,
        categoriaNombre = categoria_nombre,
        categoriaColor  = categoria_color,
        categoriaIcono  = categoria_icono,
        fecha           = fecha,
        anio            = anio.toInt(),
        mes             = mes.toInt(),
        cuotaActual     = cuota_actual?.toInt(),
        cuotaTotal      = cuota_total?.toInt(),
        idPadre         = id_padre
    )
```

- [ ] **Step 5: Ampliar la interfaz IngresoMensualRepository**

En `IngresoMensualRepository.kt`:

```kotlin
    suspend fun obtenerEnRango(desdeKey: Long, hastaKey: Long): List<IngresoMensual>
```

- [ ] **Step 6: Implementar en IngresoMensualRepositoryImpl**

```kotlin
    override suspend fun obtenerEnRango(desdeKey: Long, hastaKey: Long): List<IngresoMensual> =
        withContext(Dispatchers.Default) {
            queries.obtenerEnRango(desdeKey = desdeKey, hastaKey = hastaKey)
                .executeAsList()
                .map { com.angel.gg.db.IngresoMensual(it.id, it.anio.toInt(), it.mes.toInt(), it.monto) }
        }
```

(Verificar el nombre de la clase generada por SQLDelight — típicamente también se llama `IngresoMensual`; si difiere, ajustar.)

- [ ] **Step 7: Gate de compilación**

Run: `.\gradlew.bat :composeApp:compileKotlinJvm :composeApp:compileDebugKotlinAndroid`
Expected: BUILD SUCCESSFUL. (Si el mapper generado no coincide, revisar el paquete `com.angel.gg.db` generado.)

---

### Task 4: ExportarGastosUseCase

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/angel/gg/domain/usecase/ExportarGastosUseCase.kt`

**Interfaces:**
- Consumes: `GastoRepository.obtenerPorRango(String, String): List<Gasto>`, `IngresoMensualRepository.obtenerEnRango(Long, Long): List<IngresoMensual>`, `ExportarAlcance`, `ExportarDatos` (Task 2).
- Produces: `class ExportarGastosUseCase(gastoRepository, ingresoMensualRepository) { suspend fun ejecutar(alcance: ExportarAlcance, desde: LocalDate?, hasta: LocalDate?): ExportarDatos }`

- [ ] **Step 1: Implementar el use case**

```kotlin
package com.angel.gg.domain.usecase

import com.angel.gg.domain.model.ExportarAlcance
import com.angel.gg.domain.model.ExportarDatos
import com.angel.gg.domain.repository.GastoRepository
import com.angel.gg.domain.repository.IngresoMensualRepository
import java.time.LocalDate

class ExportarGastosUseCase(
    private val gastoRepository: GastoRepository,
    private val ingresoMensualRepository: IngresoMensualRepository
) {
    suspend fun ejecutar(
        alcance: ExportarAlcance,
        desde: LocalDate?,
        hasta: LocalDate?
    ): ExportarDatos {
        val hoy = LocalDate.now()
        val (fDesde, fHasta) = when (alcance) {
            ExportarAlcance.MES_ACTUAL -> {
                val m = hoy.withDayOfMonth(1)
                Pair(m, m.plusMonths(1).minusDays(1))
            }
            ExportarAlcance.ANIO_ACTUAL ->
                Pair(LocalDate.of(hoy.year, 1, 1), LocalDate.of(hoy.year, 12, 31))
            ExportarAlcance.TODO ->
                Pair(LocalDate.of(1900, 1, 1), LocalDate.of(9999, 12, 31))
            ExportarAlcance.PERSONALIZADO -> {
                val d = requireNotNull(desde) { "Rango personalizado sin fecha desde" }
                val h = requireNotNull(hasta) { "Rango personalizado sin fecha hasta" }
                require(!h.isBefore(d)) { "desde debe ser <= hasta" }
                Pair(d, h)
            }
        }

        val gastos = gastoRepository.obtenerPorRango(fDesde.toString(), fHasta.toString())
        val desdeKey = fDesde.year * 100L + fDesde.monthValue
        val hastaKey = fHasta.year * 100L + fHasta.monthValue
        val ingresoTotal = ingresoMensualRepository.obtenerEnRango(desdeKey, hastaKey)
            .sumOf { it.monto }

        return ExportarDatos(
            gastos = gastos,
            ingresoTotal = ingresoTotal,
            desde = fDesde,
            hasta = fHasta,
            alcance = alcance
        )
    }
}
```

- [ ] **Step 2: Gate de compilación**

Run: `.\gradlew.bat :composeApp:compileKotlinJvm :composeApp:compileDebugKotlinAndroid`
Expected: BUILD SUCCESSFUL.

---

### Task 5: expect `ExportarGuardado` + actual JVM (Desktop)

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/angel/gg/export/ExportarGuardado.kt`
- Create: `composeApp/src/jvmMain/kotlin/com/angel/gg/export/ExportarGuardado.jvm.kt`

**Interfaces:**
- Produces: `expect class ExportarGuardado { suspend fun guardar(nombreSugerido: String, bytes: ByteArray): Boolean }` (cancelar o sin destino → `false`).

> Gate de Android de esta tarea NO se pide: aún no existe el actual Android (Task 6).

- [ ] **Step 1: Declarar la expect (commonMain)**

```kotlin
package com.angel.gg.export

expect class ExportarGuardado {
    suspend fun guardar(nombreSugerido: String, bytes: ByteArray): Boolean
}
```

- [ ] **Step 2: Implementar el actual JVM**

```kotlin
package com.angel.gg.export

import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.swing.Swing

actual class ExportarGuardado {
    actual suspend fun guardar(nombreSugerido: String, bytes: ByteArray): Boolean =
        withContext(Dispatchers.Swing) {
            val chooser = JFileChooser().apply {
                dialogTitle = "Exportar xlsx"
                selectedFile = File(nombreSugerido)
                fileFilter = FileNameExtensionFilter("Excel (.xlsx)", "xlsx")
            }
            if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                val destino = chooser.selectedFile
                val archivo = if (destino.name.endsWith(".xlsx", ignoreCase = true))
                    destino else File(destino.parentFile, "${destino.name}.xlsx")
                runCatching { archivo.writeBytes(bytes) }.isSuccess
            } else false
        }
}
```

- [ ] **Step 3: Gate de compilación JVM**

Run: `.\gradlew.bat :composeApp:compileKotlinJvm`
Expected: BUILD SUCCESSFUL (Android compila en Task 6).

---

### Task 6: actual Android (SAF) + registro en MainActivity + DI

**Files:**
- Create: `composeApp/src/androidMain/kotlin/com/angel/gg/export/ExportarGuardado.android.kt`
- Modify: `composeApp/src/androidMain/kotlin/com/angel/gg/MainActivity.kt`
- Modify: `composeApp/src/androidMain/kotlin/com/angel/gg/di/AndroidModule.kt`
- Modify: `composeApp/src/jvmMain/kotlin/com/angel/gg/di/DesktopModule.kt`

**Interfaces:**
- Consumes: `expect class ExportarGuardado` (Task 5).
- Produces: bean Koin `single<ExportarGuardado> { ExportarGuardado() }` en `androidModule` y `desktopModule`.

- [ ] **Step 1: Implementar el actual Android + puente**

```kotlin
package com.angel.gg.export

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Puente entre el launcher de SAF (registrado en MainActivity) y el suspend guardar
object ExportarPuente {
    private var launcher: ((String) -> Unit)? = null
    private var contexto: Context? = null
    private var pendiente: CompletableDeferred<Uri?>? = null

    fun registrar(lanzar: (String) -> Unit, ctx: Context) {
        launcher = lanzar
        contexto = ctx
    }

    fun resolver(uri: Uri?) {
        pendiente?.complete(uri)
        pendiente = null
    }

    suspend fun guardar(nombreSugerido: String, bytes: ByteArray): Boolean {
        launcher ?: return false
        val d = CompletableDeferred<Uri?>()
        pendiente = d
        launcher?.invoke(nombreSugerido)
        val uri = d.await() ?: return false
        val ctx = contexto ?: return false
        return withContext(Dispatchers.IO) {
            try {
                ctx.contentResolver.openOutputStream(uri)?.use { it.write(bytes) } != null
            } catch (e: Exception) {
                false
            }
        }
    }
}

actual class ExportarGuardado {
    actual suspend fun guardar(nombreSugerido: String, bytes: ByteArray): Boolean =
        ExportarPuente.guardar(nombreSugerido, bytes)
}
```

- [ ] **Step 2: Registrar el launcher en MainActivity**

```kotlin
package com.angel.gg

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import com.arkivanov.decompose.defaultComponentContext
import com.angel.gg.di.androidModule
import com.angel.gg.di.initKoin
import com.angel.gg.export.ExportarPuente
import com.angel.gg.presentation.navigation.RootComponent

class MainActivity : ComponentActivity() {

    private val crearDocumento = registerForActivityResult(
        ActivityResultContracts.CreateDocument(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        )
    ) { uri -> ExportarPuente.resolver(uri) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ExportarPuente.registrar({ crearDocumento.launch(it) }, applicationContext)

        val root = RootComponent(defaultComponentContext())

        setContent {
            App(root = root)
        }
    }
}
```

- [ ] **Step 3: Registrar el bean de plataforma en DI**

AndroidModule:
```kotlin
import com.angel.gg.export.ExportarGuardado
// dentro del module:
    single<ExportarGuardado> { ExportarGuardado() }
```

DesktopModule:
```kotlin
import com.angel.gg.export.ExportarGuardado
// dentro del module:
    single<ExportarGuardado> { ExportarGuardado() }
```

- [ ] **Step 4: Gate de compilación completo**

Run: `.\gradlew.bat :composeApp:compileKotlinJvm :composeApp:compileDebugKotlinAndroid`
Expected: BUILD SUCCESSFUL. (Ahora ambas plataformas compilan con todas las Tasks 1-6.)

---

### Task 7: ExportarViewModel + DI

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/angel/gg/presentation/viewmodel/ExportarViewModel.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/angel/gg/di/AppModule.kt`

**Interfaces:**
- Consumes: `ExportarGastosUseCase` (Task 4), `XlsxGenerador` + `ExportarContenido` (Task 2), `ExportarGuardado` (Tasks 5-6), `Strings.t`/`Strings.mes`.
- Produces: `data class ExportarUiState(...)`; `class ExportarViewModel` con `state: StateFlow<ExportarUiState>`, `fun exportar(alcance: ExportarAlcance, desde: LocalDate?, hasta: LocalDate?)`, `fun guardar()`, `fun limpiarMensaje()`.

- [ ] **Step 1: Implementar el ViewModel**

```kotlin
package com.angel.gg.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.angel.gg.domain.model.ExportarAlcance
import com.angel.gg.domain.model.ExportarContenido
import com.angel.gg.domain.usecase.ExportarGastosUseCase
import com.angel.gg.export.ExportarGuardado
import com.angel.gg.export.XlsxGenerador
import com.angel.gg.presentation.i18n.Strings
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ExportarUiState(
    val exportando: Boolean = false,
    val listoParaGuardar: Boolean = false,
    val bytes: ByteArray? = null,
    val nombreArchivo: String? = null,
    val mensaje: String? = null,
    val esError: Boolean = false
)

class ExportarViewModel(
    private val exportarGastosUseCase: ExportarGastosUseCase,
    private val exportarGuardado: ExportarGuardado
) : ViewModel() {

    private val _state = MutableStateFlow(ExportarUiState())
    val state: StateFlow<ExportarUiState> = _state

    fun exportar(alcance: ExportarAlcance, desde: LocalDate?, hasta: LocalDate?) {
        viewModelScope.launch {
            _state.update { it.copy(exportando = true, mensaje = null, bytes = null, listoParaGuardar = false) }
            runCatching { exportarGastosUseCase.ejecutar(alcance, desde, hasta) }
                .onSuccess { datos ->
                    if (datos.gastos.isEmpty()) {
                        _state.update {
                            it.copy(exportando = false, mensaje = Strings.t("exportar.sinDatos"), esError = true)
                        }
                    } else {
                        val contenido = ExportarContenido(
                            hoja = hojaDe(datos.alcance, datos),
                            nombreArchivo = archivoDe(datos.alcance, datos),
                            gastos = datos.gastos,
                            ingresoTotal = datos.ingresoTotal
                        )
                        val bytes = XlsxGenerador.generar(contenido)
                        _state.update {
                            it.copy(
                                exportando = false, bytes = bytes,
                                nombreArchivo = contenido.nombreArchivo, listoParaGuardar = true
                            )
                        }
                    }
                }
                .onFailure {
                    _state.update { it.copy(exportando = false, mensaje = Strings.t("exportar.error"), esError = true) }
                }
        }
    }

    fun guardar() {
        val actual = _state.value
        val bytes = actual.bytes ?: return
        val nombre = actual.nombreArchivo ?: return
        viewModelScope.launch {
            val ok = exportarGuardado.guardar(nombre, bytes)
            _state.update {
                it.copy(
                    listoParaGuardar = false, bytes = null, nombreArchivo = null,
                    mensaje = if (ok) Strings.t("exportar.exito") else null,
                    esError = !ok
                )
            }
        }
    }

    fun limpiarMensaje() {
        _state.update { it.copy(mensaje = null, esError = false) }
    }

    private fun hojaDe(alcance: ExportarAlcance, datos: com.angel.gg.domain.model.ExportarDatos): String =
        when (alcance) {
            ExportarAlcance.MES_ACTUAL -> "${Strings.mes(datos.desde.monthValue)} ${datos.desde.year}"
            ExportarAlcance.ANIO_ACTUAL -> datos.desde.year.toString()
            else -> "Gastos"
        }

    private fun archivoDe(alcance: ExportarAlcance, datos: com.angel.gg.domain.model.ExportarDatos): String =
        when (alcance) {
            ExportarAlcance.MES_ACTUAL -> "Gastos ${Strings.mes(datos.desde.monthValue)} ${datos.desde.year}.xlsx"
            ExportarAlcance.ANIO_ACTUAL -> "Gastos ${datos.desde.year}.xlsx"
            ExportarAlcance.TODO -> "Gastos.xlsx"
            ExportarAlcance.PERSONALIZADO -> "Gastos ${datos.desde} a ${datos.hasta}.xlsx"
        }
}
```

- [ ] **Step 2: Registrar en DI (AppModule)**

Agregar en `domainModule`:

```kotlin
    factory {
        ExportarGastosUseCase(
            gastoRepository = get(),
            ingresoMensualRepository = get()
        )
    }
```

Y en `viewModelModule`:

```kotlin
    factory {
        ExportarViewModel(
            exportarGastosUseCase = get(),
            exportarGuardado = get()
        )
    }
```

Con los imports correspondientes:
```kotlin
import com.angel.gg.domain.usecase.ExportarGastosUseCase
import com.angel.gg.export.ExportarGuardado
import com.angel.gg.presentation.viewmodel.ExportarViewModel
```

- [ ] **Step 3: Gate de compilación**

Run: `.\gradlew.bat :composeApp:compileKotlinJvm :composeApp:compileDebugKotlinAndroid`
Expected: BUILD SUCCESSFUL.

---

### Task 8: Claves i18n `exportar.*` (ES/EN/FR)

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/angel/gg/presentation/i18n/EsStrings.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/angel/gg/presentation/i18n/EnStrings.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/angel/gg/presentation/i18n/FrStrings.kt`

- [ ] **Step 1: Agregar las claves (EsStrings)**

```kotlin
        // Exportar
        "exportar.titulo"       to "Exportar a Excel",
        "exportar.mesActual"    to "Mes actual",
        "exportar.anioActual"   to "Año actual",
        "exportar.todo"         to "Todo",
        "exportar.personalizado" to "Personalizado",
        "exportar.desde"        to "Desde",
        "exportar.hasta"        to "Hasta",
        "exportar.boton"        to "Exportar",
        "exportar.exito"        to "Exportación completada",
        "exportar.error"        to "No se pudo exportar",
        "exportar.sinDatos"     to "No hay gastos en este período"
```

- [ ] **Step 2: Agregar las claves (EnStrings)**

```kotlin
        // Export
        "exportar.titulo"       to "Export to Excel",
        "exportar.mesActual"    to "Current month",
        "exportar.anioActual"   to "Current year",
        "exportar.todo"         to "All",
        "exportar.personalizado" to "Custom",
        "exportar.desde"        to "From",
        "exportar.hasta"        to "To",
        "exportar.boton"        to "Export",
        "exportar.exito"        to "Export completed",
        "exportar.error"        to "Export failed",
        "exportar.sinDatos"     to "No expenses in this period"
```

- [ ] **Step 3: Agregar las claves (FrStrings)**

```kotlin
        // Exportation
        "exportar.titulo"       to "Exporter vers Excel",
        "exportar.mesActual"    to "Mois actuel",
        "exportar.anioActual"   to "Année actuelle",
        "exportar.todo"         to "Tout",
        "exportar.personalizado" to "Personnalisée",
        "exportar.desde"        to "Depuis",
        "exportar.hasta"        to "Jusqu'à",
        "exportar.boton"        to "Exporter",
        "exportar.exito"        to "Exportation terminée",
        "exportar.error"        to "Échec de l'exportation",
        "exportar.sinDatos"     to "Aucune dépense sur cette période"
```

- [ ] **Step 4: Gate de compilación**

Run: `.\gradlew.bat :composeApp:compileKotlinJvm :composeApp:compileDebugKotlinAndroid`
Expected: BUILD SUCCESSFUL.

---

### Task 9: Diálogo de exportación + wiring en AjustesScreen

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/angel/gg/presentation/screen/ExportarDialog.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/angel/gg/presentation/screen/AjustesScreen.kt`

**Interfaces:**
- Consumes: `ExportarViewModel` (Task 7), `ExportarAlcance` (Task 2), `tr(...)`/`Strings.t(...)`.
- Produces:
  - `@Composable fun ExportarDialog(onDismiss: () -> Unit, viewModel: ExportarViewModel)`.
  - AjustesScreen: fila "Exportar" abre el diálogo; SnackbarHost muestra `mensaje` del VM.

- [ ] **Step 1: Implementar ExportarDialog**

```kotlin
package com.angel.gg.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.angel.gg.domain.model.ExportarAlcance
import com.angel.gg.presentation.AppTheme
import com.angel.gg.presentation.i18n.tr
import com.angel.gg.presentation.viewmodel.ExportarViewModel
import java.time.LocalDate
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportarDialog(
    onDismiss: () -> Unit,
    viewModel: ExportarViewModel = koinInject()
) {
    val estado by viewModel.state.collectAsState()

    var alcance by remember { mutableStateOf(ExportarAlcance.MES_ACTUAL) }
    var desde by remember { mutableStateOf<LocalDate?>(null) }
    var hasta by remember { mutableStateOf<LocalDate?>(null) }
    var editarCampo by remember { mutableStateOf<Boolean?>(null) } // true=desde, false=hasta

    // Una vez generados los bytes, disparar el guardado (JFileChooser o SAF)
    LaunchedEffect(estado.listoParaGuardar) {
        if (estado.listoParaGuardar) viewModel.guardar()
    }

    // Cerrar el diálogo al confirmar el guardado
    LaunchedEffect(estado.mensaje) {
        val m = estado.mensaje
        if (m != null && !estado.esError) onDismiss()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppTheme.CardDark,
        title = { Text(tr("exportar.titulo"), color = AppTheme.TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ExportarAlcance.entries.forEach { opcion ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = alcance == opcion,
                            onClick = { alcance = opcion },
                            colors = RadioButtonDefaults.colors(selectedColor = AppTheme.Indigo)
                        )
                        Text(
                            when (opcion) {
                                ExportarAlcance.MES_ACTUAL -> tr("exportar.mesActual")
                                ExportarAlcance.ANIO_ACTUAL -> tr("exportar.anioActual")
                                ExportarAlcance.TODO -> tr("exportar.todo")
                                ExportarAlcance.PERSONALIZADO -> tr("exportar.personalizado")
                            },
                            color = AppTheme.TextPrimary,
                            fontSize = 15.sp
                        )
                    }
                }

                if (alcance == ExportarAlcance.PERSONALIZADO) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        FechaCampo(
                            etiqueta = tr("exportar.desde"), valor = desde,
                            modifier = Modifier.weight(1f)
                        ) { editarCampo = true }
                        FechaCampo(
                            etiqueta = tr("exportar.hasta"), valor = hasta,
                            modifier = Modifier.weight(1f)
                        ) { editarCampo = false }
                    }
                }

                if (estado.exportando) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(8.dp),
                            color = AppTheme.Indigo
                        )
                    }
                }

                estado.mensaje?.let {
                    if (estado.esError) {
                        Text(it, color = AppTheme.Red, fontSize = 13.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !estado.exportando && alcanceValido(alcance, desde, hasta),
                onClick = { viewModel.exportar(alcance, desde, hasta) }
            ) {
                Text(tr("exportar.boton"), color = if (estado.exportando) AppTheme.TextSubtle else AppTheme.Indigo)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(tr("comun.cancelar"), color = AppTheme.TextMuted)
            }
        }
    )

    if (alcance == ExportarAlcance.PERSONALIZADO && editarCampo != null) {
        val campo = editarCampo
        val fechaInicial = if (campo == true) desde else hasta
        val millisInicial = fechaInicial?.atStartOfDay(java.time.ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
        val state = rememberDatePickerState(initialSelectedDateMillis = millisInicial)
        DatePickerDialog(
            onDismissRequest = { editarCampo = null },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        val fecha = java.time.Instant.ofEpochMilli(millis)
                            .atZone(java.time.ZoneOffset.UTC)
                            .toLocalDate()
                        if (campo == true) desde = fecha else hasta = fecha
                    }
                    editarCampo = null
                }) { Text("OK", color = AppTheme.Indigo) }
            },
            dismissButton = {
                TextButton(onClick = { editarCampo = null }) {
                    Text(tr("comun.cancelar"), color = AppTheme.TextMuted)
                }
            }
        ) {
            DatePicker(state = state)
        }
    }
}

@Composable
private fun FechaCampo(
    etiqueta: String,
    valor: LocalDate?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(modifier = modifier) {
        Text(etiqueta, color = AppTheme.TextMuted, fontSize = 12.sp)
        OutlinedTextField(
            value = valor?.toString() ?: "",
            readOnly = true,
            onValueChange = {},
            onClick = onClick,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AppTheme.Indigo,
                unfocusedBorderColor = AppTheme.BorderColor,
                focusedTextColor = AppTheme.TextPrimary,
                unfocusedTextColor = AppTheme.TextPrimary
            )
        )
    }
}

private fun alcanceValido(
    alcance: ExportarAlcance,
    desde: LocalDate?,
    hasta: LocalDate?
): Boolean = when (alcance) {
    ExportarAlcance.PERSONALIZADO -> desde != null && hasta != null && !hasta.isBefore(desde)
    else -> true
}

> Nota: `OutlinedTextField(onClick=...)` es API M3 reciente (1.10 alpha): si el compilador no la acepta, envolver el `OutlinedTextField` en un `Box(Modifier.clickable { onClick() })`. `clickableNoRipple` necesita `import androidx.compose.foundation.clickable`.

- [ ] **Step 2: Wire en AjustesScreen**

Modificar `AjustesScreen.kt`:

```kotlin
    var abrirExportar by remember { mutableStateOf(false) }
    val exportarViewModel: ExportarViewModel = koinInject()
    val exportarEstado by exportarViewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(exportarEstado.mensaje) {
        exportarEstado.mensaje?.let {
            snackbarHostState.showSnackbar(it)
            exportarViewModel.limpiarMensaje()
        }
    }
```

Cambiar el `Scaffold` para incluir el snackbar host:

```kotlin
    Scaffold(
        containerColor = AppTheme.BgDark,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { ... }
    ) { padding -> ... }
```

Cambiar el `onClick` de la fila Exportar (dentro del último Card):

```kotlin
                    AjustesRow(
                        AppIcons.Exportar,       tr("ajustes.exportar"), tr("ajustes.exportarSub"),
                        AppTheme.Orange,         onClick = { abrirExportar = true }
                    )
```

Y justo antes del cierre del Scaffold (`) { padding ->` … `}`):

```kotlin
            if (abrirExportar) {
                ExportarDialog(
                    onDismiss = { abrirExportar = false; exportarViewModel.limpiarMensaje() }
                )
            }
```

Imports nuevos en AjustesScreen.kt:

```kotlin
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import com.angel.gg.presentation.viewmodel.ExportarViewModel
import org.koin.compose.koinInject
```

- [ ] **Step 3: Gate de compilación + tests completos**

Run: `.\gradlew.bat :composeApp:jvmTest`
Run: `.\gradlew.bat :composeApp:compileKotlinJvm :composeApp:compileDebugKotlinAndroid`
Expected: PASS + BUILD SUCCESSFUL.

---

## Verification final (manual)

1. **Desktop:** `.\gradlew.bat :composeApp:run` → Ajustes → Exportar → Mes actual → JFileChooser → guardar → abrir en Excel/WPS. Verificar: columnas A-E, G/I, fechas `dd/mm/yyyy`, balance `G2-SUM(A:A)`, porcentaje `FIXED(...)&"%"` en I3, autofiltro.
2. Repetir para: Año actual, Todo y Personalizado (con rango desde/hasta).
3. **Android:** instalar APK (`:composeApp:assembleDebug`) → Ajustes → Exportar → SAF pide destino → archivo guardado.

## Fuera de alcance (iteración futura)
- Gráfico circular embebido (Chart2 del ejemplo).
- Importación desde archivo.