package com.angel.gg.export

import com.angel.gg.domain.model.ExportarContenido
import com.angel.gg.domain.model.HojaExcel
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

        val hojasXml = contenido.hojas.mapIndexed { i, hoja ->
            "xl/worksheets/sheet${i + 1}.xml" to hojaXml(hoja, ::idx).toByteArray()
        }

        return ZipWriter.zip(listOf(
            "[Content_Types].xml"        to contentTypesXml(contenido.hojas.size).toByteArray(),
            "_rels/.rels"                to rootRelsXml().toByteArray(),
            "xl/workbook.xml"            to workbookXml(contenido.hojas).toByteArray(),
            "xl/_rels/workbook.xml.rels" to workbookRelsXml(contenido.hojas.size).toByteArray(),
            "xl/sharedStrings.xml"       to sharedStringsXml(textos).toByteArray(),
            "xl/styles.xml"              to stylesXml().toByteArray()
        ) + hojasXml)
    }

    private fun hojaXml(hoja: HojaExcel, idx: (String) -> Int): String {
        val n = hoja.gastos.size
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

        sheet.append("<row r=\"1\">")
        listOf("A", "B", "C", "D", "E").forEachIndexed { i, col ->
            sheet.append("<c r=\"${col}1\" s=\"1\" t=\"s\"><v>${idx(headerDe(i))}</v></c>")
        }
        sheet.append("<c r=\"F1\" s=\"2\"/>")
        sheet.append("<c r=\"G1\" s=\"1\" t=\"s\"><v>${idx("Saldo del mes")}</v></c>")
        sheet.append("<c r=\"H1\" s=\"2\"/>")
        sheet.append("<c r=\"I1\" s=\"1\" t=\"s\"><v>${idx("Balance disponible")}</v></c>")
        sheet.append("</row>")

        hoja.gastos.forEachIndexed { i, gasto ->
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
            if (r == 2 && hoja.ingresoTotal > 0) {
                sheet.append("<c r=\"G2\" s=\"1\" t=\"n\"><v>${formatearNumero(hoja.ingresoTotal)}</v></c>")
            }
            sheet.append("<c r=\"H$r\" s=\"2\"/>")
            if (r == 2 && hoja.ingresoTotal > 0) {
                sheet.append("<c r=\"I2\" s=\"1\"><f>G2-SUM(A:A)</f><v>${formatearNumero(balanceDe(hoja))}</v></c>")
            }
            sheet.append("</row>")
        }

        if (hoja.ingresoTotal > 0) {
            sheet.append("<row r=\"3\"><c r=\"I3\" s=\"1\" t=\"str\"><f>FIXED((I2/G2)*100.2)&amp;\"%\"</f><v>${porcentajeDe(hoja)}</v></c></row>")
        }

        sheet.append("</sheetData>")
        sheet.append("<autoFilter ref=\"A1:E$ultimaFila\"/>")
        sheet.append("</worksheet>")
        return sheet.toString()
    }

    private fun headerDe(indice: Int): String =
        listOf("Monto", "Descripcion", "Categoria", "Fecha", "Cuota")[indice]

    private fun balanceDe(hoja: HojaExcel): Double =
        hoja.ingresoTotal - hoja.gastos.sumOf { it.monto }

    private fun porcentajeDe(hoja: HojaExcel): String {
        val b = balanceDe(hoja)
        if (hoja.ingresoTotal <= 0) return "0,00%"
        val valor = (b / hoja.ingresoTotal) * 100.2
        return (String.format("%.2f", valor).replace('.', ',')) + "%"
    }

    private fun contentTypesXml(numHojas: Int): String {
        val overrides = StringBuilder()
        overrides.append("<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>")
        (1..numHojas).forEach {
            overrides.append("<Override PartName=\"/xl/worksheets/sheet$it.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>")
        }
        overrides.append("<Override PartName=\"/xl/sharedStrings.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sharedStrings+xml\"/>")
        overrides.append("<Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>")
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
<Default Extension="xml" ContentType="application/xml"/>
$overrides
</Types>"""
    }

    private fun rootRelsXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""

    private fun workbookXml(hojas: List<HojaExcel>): String {
        val sheets = hojas.mapIndexed { i, hoja ->
            "<sheet name=\"${hoja.nombre}\" sheetId=\"${i + 1}\" r:id=\"rId${i + 1}\"/>"
        }.joinToString("")
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"
 xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
<sheets>$sheets</sheets>
</workbook>"""
    }

    private fun workbookRelsXml(numHojas: Int): String {
        val rels = StringBuilder()
        (1..numHojas).forEach {
            rels.append("<Relationship Id=\"rId$it\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet$it.xml\"/>")
        }
        rels.append("<Relationship Id=\"rId${numHojas + 1}\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/sharedStrings\" Target=\"sharedStrings.xml\"/>")
        rels.append("<Relationship Id=\"rId${numHojas + 2}\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/>")
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
$rels
</Relationships>"""
    }

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
