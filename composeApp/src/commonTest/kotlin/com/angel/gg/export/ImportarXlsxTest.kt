package com.angel.gg.export

import com.angel.gg.domain.model.ResultadoImportacion
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ImportarXlsxTest {

    private data class GastoTest(
        val monto: Double,
        val descripcion: String,
        val categoria: String,
        val fecha: LocalDate,
        val cuota: Boolean = false
    )

    private data class HojaTest(
        val nombre: String,
        val gastos: List<GastoTest>,
        val ingreso: Double? = null
    )

    private fun construirLibro(hojas: List<HojaTest>): ByteArray {
        val textos = ArrayList<String>()
        fun idx(t: String): Int = textos.indexOf(t).let {
            if (it != -1) it else { textos.add(t); textos.size - 1 }
        }

        val sheetsXml = hojas.mapIndexed { i, hoja ->
            val sb = StringBuilder()
            sb.append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetData>")
            sb.append("<row r=\"1\">")
            listOf("Monto", "Descripcion", "Categoria", "Fecha", "Cuota").forEachIndexed { j, cab ->
                sb.append("<c r=\"${('A'.code + j).toChar()}1\" s=\"1\" t=\"s\"><v>${idx(cab)}</v></c>")
            }
            sb.append("</row>")
            hoja.gastos.forEachIndexed { j, g ->
                val r = j + 2
                val serial = (g.fecha.toEpochDay() - LocalDate.of(1899, 12, 30).toEpochDay())
                sb.append("<row r=\"$r\">")
                sb.append("<c r=\"A$r\" s=\"1\" t=\"n\"><v>${g.monto}</v></c>")
                sb.append("<c r=\"B$r\" s=\"1\" t=\"s\"><v>${idx(g.descripcion)}</v></c>")
                sb.append("<c r=\"C$r\" s=\"3\" t=\"s\"><v>${idx(g.categoria)}</v></c>")
                sb.append("<c r=\"D$r\" s=\"4\" t=\"n\"><v>$serial</v></c>")
                if (g.cuota) sb.append("<c r=\"E$r\" s=\"3\" t=\"s\"><v>${idx("Cuota 1/6")}</v></c>")
                else sb.append("<c r=\"E$r\" s=\"2\"/>")
                if (r == 2 && hoja.ingreso != null) sb.append("<c r=\"G2\" s=\"1\" t=\"n\"><v>${hoja.ingreso}</v></c>")
                sb.append("</row>")
            }
            sb.append("</sheetData></worksheet>")
            "xl/worksheets/sheet${i + 1}.xml" to sb.toString().toByteArray(Charsets.UTF_8)
        }

        val workbookXml = "<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" " +
            "xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><sheets>" +
            hojas.mapIndexed { i, hoja -> "<sheet name=\"${hoja.nombre}\" sheetId=\"${i + 1}\" r:id=\"rId${i + 1}\"/>" }.joinToString("") +
            "</sheets></workbook>"

        val relsXml = "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
            hojas.indices.joinToString("") {
                "<Relationship Id=\"rId${it + 1}\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet${it + 1}.xml\"/>"
            } +
            "<Relationship Id=\"rId${hojas.size + 1}\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/sharedStrings\" Target=\"sharedStrings.xml\"/>" +
            "</Relationships>"

        val sharedXml = "<sst xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" count=\"${textos.size}\" uniqueCount=\"${textos.size}\">" +
            textos.joinToString("") { "<si><t>${it.replace("&", "&amp;")}</t></si>" } +
            "</sst>"

        return ZipWriter.zip(listOf(
            "[Content_Types].xml" to "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"><Default Extension=\"xml\" ContentType=\"application/xml\"/></Types>".toByteArray(),
            "_rels/.rels" to "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"/>".toByteArray(),
            "xl/workbook.xml" to workbookXml.toByteArray(Charsets.UTF_8),
            "xl/_rels/workbook.xml.rels" to relsXml.toByteArray(Charsets.UTF_8),
            "xl/sharedStrings.xml" to sharedXml.toByteArray(Charsets.UTF_8)
        ) + sheetsXml)
    }

    private val sep2026 = LocalDate.of(2026, 9, 1)
    private val sep2027 = LocalDate.of(2027, 9, 1)
    private val oct2026 = LocalDate.of(2026, 10, 1)

    @Test
    fun lecturaParseaLasFilasDeVariasHojas() {
        val libro = construirLibro(listOf(
            HojaTest("Sep 2026", listOf(
                GastoTest(100.0, "Pago mensual", "Luz", sep2026),
                GastoTest(222.0, "Depto & Internet", "Departamento", sep2026, cuota = true)
            )),
            HojaTest("Oct 2026", listOf(GastoTest(50.0, "Taxi", "Transporte", oct2026)))
        ))

        val r = assertIs<ImportarXlsx.ResultadoLectura.Ok>(ImportarXlsx.leer(libro))

        assertEquals(3, r.datos.filas.size)
        val primera = r.datos.filas[0]
        assertEquals(100.0, primera.monto)
        assertEquals("Pago mensual", primera.descripcion)
        assertEquals("Luz", primera.categoriaNombre)
        assertEquals(sep2026, primera.fecha)
        assertTrue(!primera.esCuota)
        val cuota = r.datos.filas[1]
        assertTrue(cuota.esCuota)
        assertEquals("Depto & Internet", cuota.descripcion)
        assertEquals(oct2026, r.datos.filas[2].fecha)
    }

    @Test
    fun lecturaAsociaIngresoAlMesDeLaHoja() {
        val libro = construirLibro(listOf(
            HojaTest("Sep 2026", listOf(GastoTest(100.0, "Pago mensual", "Luz", sep2026)), ingreso = 3565.0),
            HojaTest("Sep 2027", listOf(GastoTest(200.0, "Renta", "Casa", sep2027)))
        ))

        val r = assertIs<ImportarXlsx.ResultadoLectura.Ok>(ImportarXlsx.leer(libro))

        assertEquals(1, r.datos.ingresosPorMes.size)
        val ing = r.datos.ingresosPorMes[0]
        assertEquals(2026, ing.anio)
        assertEquals(9, ing.mes)
        assertEquals(3565.0, ing.monto)
    }

    @Test
    fun lecturaDescodificaEntidadesXml() {
        val libro = construirLibro(listOf(
            HojaTest("Sep 2026", listOf(GastoTest(10.0, "Comida & Bebida", "Alimentación", sep2026)))
        ))

        val r = assertIs<ImportarXlsx.ResultadoLectura.Ok>(ImportarXlsx.leer(libro))

        assertEquals("Comida & Bebida", r.datos.filas[0].descripcion)
    }

    @Test
    fun lecturaConZipInvalidoDaArchivoInvalido() {
        assertIs<ImportarXlsx.ResultadoLectura.ArchivoInvalido>(
            ImportarXlsx.leer("esto no es un zip".toByteArray(Charsets.UTF_8))
        )
    }

    @Test
    fun lecturaOmiteFilaEncabezado() {
        val libro = construirLibro(listOf(HojaTest("Sep 2026", emptyList())))
        val r = assertIs<ImportarXlsx.ResultadoLectura.Ok>(ImportarXlsx.leer(libro))
        assertEquals(0, r.datos.filas.size)
    }

    private fun construirLibroConRowsXML(
        rowsXml: String,
        sharedTextos: List<String> = emptyList()
    ): ByteArray {
        val sheet = "<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetData>${rowsXml}</sheetData></worksheet>"
        val workbook = "<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" " +
            "xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><sheets>" +
            "<sheet name=\"Sep 2026\" sheetId=\"1\" r:id=\"rId1\"/></sheets></workbook>"
        val rels = "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
            "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/>" +
            "<Relationship Id=\"rId2\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/sharedStrings\" Target=\"sharedStrings.xml\"/>" +
            "</Relationships>"
        val shared = "<sst xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" count=\"0\" uniqueCount=\"0\">" +
            sharedTextos.joinToString("") { "<si><t>${it.replace("&", "&amp;")}</t></si>" } +
            "</sst>"
        return ZipWriter.zip(listOf(
            "[Content_Types].xml" to "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"><Default Extension=\"xml\" ContentType=\"application/xml\"/></Types>".toByteArray(),
            "_rels/.rels" to "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"/>".toByteArray(),
            "xl/workbook.xml" to workbook.toByteArray(Charsets.UTF_8),
            "xl/_rels/workbook.xml.rels" to rels.toByteArray(Charsets.UTF_8),
            "xl/sharedStrings.xml" to shared.toByteArray(Charsets.UTF_8),
            "xl/worksheets/sheet1.xml" to sheet.toByteArray(Charsets.UTF_8)
        ))
    }

    private fun serialDe(fecha: LocalDate): Long =
        fecha.toEpochDay() - LocalDate.of(1899, 12, 30).toEpochDay()

    private fun fila(
        r: Int,
        monto: Double? = null,
        desc: String? = null,
        cat: String? = null,
        fecha: LocalDate? = null
    ): String {
        val sb = StringBuilder("<row r=\"$r\">")
        if (monto != null) sb.append("<c r=\"A$r\" s=\"1\" t=\"n\"><v>$monto</v></c>")
        if (desc != null) sb.append("<c r=\"B$r\" s=\"1\" t=\"s\"><v>${desc.toInt()}</v></c>")
        if (cat != null) sb.append("<c r=\"C$r\" s=\"3\" t=\"s\"><v>${cat.toInt()}</v></c>")
        if (fecha != null) sb.append("<c r=\"D$r\" s=\"4\" t=\"n\"><v>${serialDe(fecha)}</v></c>")
        sb.append("</row>")
        return sb.toString()
    }

    @Test
    fun lectura_usaGuionComoDescripcionSiLaFilaTieneMonto() {
        val libro = construirLibroConRowsXML(
            fila(2, monto = 100.0, cat = "0", fecha = sep2026),
            sharedTextos = listOf("Luz")
        )
        val r = assertIs<ImportarXlsx.ResultadoLectura.Ok>(ImportarXlsx.leer(libro))
        assertEquals(1, r.datos.filas.size)
        assertEquals(100.0, r.datos.filas[0].monto)
        assertEquals("-", r.datos.filas[0].descripcion)
        assertEquals("Luz", r.datos.filas[0].categoriaNombre)
        assertEquals(sep2026, r.datos.filas[0].fecha)
    }

    @Test
    fun lectura_usaGuionComoCategoriaSiLaFilaTieneMonto() {
        val libro = construirLibroConRowsXML(
            fila(2, monto = 100.0, desc = "0", fecha = sep2026),
            sharedTextos = listOf("Pago mensual")
        )
        val r = assertIs<ImportarXlsx.ResultadoLectura.Ok>(ImportarXlsx.leer(libro))
        assertEquals(1, r.datos.filas.size)
        assertEquals("Pago mensual", r.datos.filas[0].descripcion)
        assertEquals("-", r.datos.filas[0].categoriaNombre)
    }

    @Test
    fun lectura_usaPrimeroDelMesActualSiLaFilaTieneMonto() {
        val libro = construirLibroConRowsXML(
            fila(2, monto = 100.0, desc = "0", cat = "0"),
            sharedTextos = listOf("Pago mensual", "Alimentación")
        )
        val r = assertIs<ImportarXlsx.ResultadoLectura.Ok>(ImportarXlsx.leer(libro))
        assertEquals(1, r.datos.filas.size)
        assertEquals(LocalDate.now().withDayOfMonth(1), r.datos.filas[0].fecha)
    }

    @Test
    fun lectura_ignoraFilaSinMonto() {
        val libro = construirLibroConRowsXML(
            fila(2, desc = "0", cat = "0", fecha = sep2026),
            sharedTextos = listOf("Comida", "Luz")
        )
        val r = assertIs<ImportarXlsx.ResultadoLectura.Ok>(ImportarXlsx.leer(libro))
        assertEquals(0, r.datos.filas.size)
    }

    @Test
    fun lectura_ignoraFilasConMontoCeroONegativo() {
        val libro = construirLibroConRowsXML(
            fila(2, monto = 0.0, desc = "0", cat = "0", fecha = sep2026) +
                fila(3, monto = -5.0, desc = "0", cat = "1", fecha = sep2026),
            sharedTextos = listOf("Cero", "Negativo")
        )
        val r = assertIs<ImportarXlsx.ResultadoLectura.Ok>(ImportarXlsx.leer(libro))
        assertEquals(0, r.datos.filas.size)
    }

    @Test
    fun lectura_conMontoYFechaEnBlancoUsaPrimeroDelMes() {
        val libro = construirLibroConRowsXML(
            "<row r=\"2\"><c r=\"A2\" s=\"1\" t=\"n\"><v>85.5</v></c>" +
                "<c r=\"B2\" s=\"1\" t=\"s\"><v>0</v></c><c r=\"C2\" s=\"3\" t=\"s\"><v>1</v></c>" +
                "<c r=\"D2\" s=\"4\" t=\"n\"><v></v></c></row>",
            sharedTextos = listOf("Compra", "Gastos")
        )
        val r = assertIs<ImportarXlsx.ResultadoLectura.Ok>(ImportarXlsx.leer(libro))
        assertEquals(1, r.datos.filas.size)
        assertEquals(85.5, r.datos.filas[0].monto)
        assertEquals(LocalDate.now().withDayOfMonth(1), r.datos.filas[0].fecha)
    }

    @Test
    fun lectura_completaCadaCampoFaltanteConSuDefault() {
        val libro = construirLibroConRowsXML(
            fila(2, monto = 10.0, desc = "0", fecha = sep2026) +
                fila(3, monto = 20.0, cat = "1", fecha = sep2026) +
                fila(4, monto = 30.0, desc = "0", cat = "1"),
            sharedTextos = listOf("Con cat", "Luz")
        )
        val r = assertIs<ImportarXlsx.ResultadoLectura.Ok>(ImportarXlsx.leer(libro))
        assertEquals(3, r.datos.filas.size)
        val sinCategoria = r.datos.filas[0]
        assertEquals("-", sinCategoria.categoriaNombre)
        assertEquals("Con cat", sinCategoria.descripcion)
        val sinDescripcion = r.datos.filas[1]
        assertEquals("-", sinDescripcion.descripcion)
        assertEquals("Luz", sinDescripcion.categoriaNombre)
        val sinFecha = r.datos.filas[2]
        assertEquals(LocalDate.now().withDayOfMonth(1), sinFecha.fecha)
        assertEquals("Luz", r.datos.filas[2].categoriaNombre)
    }
}