# Task 2 Brief: Modelos + XlsxGenerador (commonMain) + tests

Extraído del plan: `D:\GestorGastos\docs\superpowers\plans\2026-09-10-exportar-xlsx.md` (Task 2, líneas 230-524). Este archivo es tu única fuente de requisitos; usa los valores exactos verbatim.

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/angel/gg/domain/model/Exportacion.kt`
- Create: `composeApp/src/commonMain/kotlin/com/angel/gg/export/XlsxGenerador.kt`
- Modify (not create!): `composeApp/src/commonTest/kotlin/com/angel/gg/export/XlsxGeneradorTest.kt`

**Interfaces:**
- Consumes: `ZipWriter.zip(List<Pair<String, ByteArray>>)` (ya existe de Task 1); `Gasto` (modelo existente `com.angel.gg.domain.model.Gasto`).
- Produces:
  - `enum ExportarAlcance { MES_ACTUAL, ANIO_ACTUAL, TODO, PERSONALIZADO }`
  - `data class ExportarDatos(val gastos: List<Gasto>, val ingresoTotal: Double, val desde: LocalDate, val hasta: LocalDate, val alcance: ExportarAlcance)`
  - `data class ExportarContenido(val hoja: String, val nombreArchivo: String, val gastos: List<Gasto>, val ingresoTotal: Double)`
  - `object XlsxGenerador { fun generar(contenido: ExportarContenido): ByteArray; fun serialFecha(fechaIso: String): String; fun formatearNumero(v: Double): String }`

## IMPORTANTE — conflicto del plan resuelto (lee antes de empezar)

El plan de Task 1 creó `composeApp/src/commonTest/kotlin/com/angel/gg/export/XlsxGeneradorTest.kt` con una clase `XlsxGeneradorTest` que contiene un solo test `zipGeneradoTieneFirmasYNombre` y el helper privado `aTexto()` (que ya fue corregido a `this@aTexto.forEach` por el implementer de Task 1 — usa ESE helper tal como está, NO lo revertas).

El texto de Task 2 en el plan re-declara `class XlsxGeneradorTest` en el mismo archivo. NO hagas eso. **Amplía la clase existente** `XlsxGeneradorTest`: manten el test de Task 1 (`zipGeneradoTieneFirmasYNombre`) y añade los 3 tests nuevos de esta tarea en la misma clase. Solo debe existir UNA clase `XlsxGeneradorTest` en el archivo.

## Step 1: Escribir los tests que fallan

Añade estos tests a la clase `XlsxGeneradorTest` existente (conservando el helper `aTexto()` y el test de Task 1 ya presentes):

```kotlin
import com.angel.gg.domain.model.ExportarContenido
import com.angel.gg.domain.model.Gasto
import kotlin.test.assertEquals
import java.time.LocalDate
```

(tests nuevos dentro de la clase):

```kotlin
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
```

Necesitarás el import `kotlin.test.assertTrue` si no está ya importado en el archivo.

## Step 2: Ejecutar los tests y verificar que fallan

Run: `.\gradlew.bat :composeApp:jvmTest`
Expected: FAIL — `ExportarAlcance`, `ExportarContenido`, `ExportarDatos`, `XlsxGenerador` no existen.

## Step 3: Crear los modelos

`composeApp/src/commonMain/kotlin/com/angel/gg/domain/model/Exportacion.kt`:

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

Verifica el constructor real de `com.angel.gg.domain.model.Gasto` (campos: id, monto, descripcion, categoriaId, categoriaNombre, categoriaColor, categoriaIcono, fecha, anio, mes, cuotaActual, cuotaTotal, idPadre). El test usa parámetros nombrados, así que solo importan los nombres y defaults.

## Step 4: Implementar XlsxGenerador

`composeApp/src/commonMain/kotlin/com/angel/gg/export/XlsxGenerador.kt`:

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

Nota del plan: `String.format` (JVM y Android lo soportan en commonMain compilando contra la JDK). SI `String.format` diera problemas para Android, sustituir por `kotlin.math.round(valor * 100) / 100.0` y concatenar — pero primero intenta con `String.format`; el gate de Android te dirá si no compila.

## Step 5: Ejecutar los tests y verificar que pasan

Run: `.\gradlew.bat :composeApp:jvmTest`
Expected: PASS de los tests (los 3 nuevos + el de Task 1).

## Step 6: Gate de compilación completo

Run: `.\gradlew.bat :composeApp:compileKotlinJvm :composeApp:compileDebugKotlinAndroid`
Expected: BUILD SUCCESSFUL.