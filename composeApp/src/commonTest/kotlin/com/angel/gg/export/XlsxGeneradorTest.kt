package com.angel.gg.export

import com.angel.gg.domain.model.ExportarContenido
import com.angel.gg.domain.model.Gasto
import com.angel.gg.domain.model.HojaExcel
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class XlsxGeneradorTest {

    private fun ByteArray.aTexto(): String =
        buildString { this@aTexto.forEach { append((it.toInt() and 0xFF).toChar()) } }

    private fun ByteArray.nombresZip(): List<String> {
        val nombres = mutableListOf<String>()
        var pos = 0
        while (pos + 30 <= size) {
            val sig =
                (this[pos].toInt() and 0xFF) or
                ((this[pos + 1].toInt() and 0xFF) shl 8) or
                ((this[pos + 2].toInt() and 0xFF) shl 16) or
                ((this[pos + 3].toInt() and 0xFF) shl 24)
            if (sig != 0x04034b50) break
            val fnamelen = (this[pos + 26].toInt() and 0xFF) or ((this[pos + 27].toInt() and 0xFF) shl 8)
            val extralen = (this[pos + 28].toInt() and 0xFF) or ((this[pos + 29].toInt() and 0xFF) shl 8)
            val compsize = (this[pos + 18].toInt() and 0xFF) or
                ((this[pos + 19].toInt() and 0xFF) shl 8) or
                ((this[pos + 20].toInt() and 0xFF) shl 16) or
                ((this[pos + 21].toInt() and 0xFF) shl 24)
            nombres.add(
                buildString { for (i in 0 until fnamelen) append((this@nombresZip[pos + 30 + i].toInt() and 0xFF).toChar()) }
            )
            pos += 30 + fnamelen + extralen + compsize
        }
        return nombres
    }

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
        nombreArchivo = "Gastos Sep 2026.xlsx",
        hojas = listOf(HojaExcel(nombre = "Sep 2026", gastos = gastos, ingresoTotal = 3565.0))
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

    @Test
    fun generadorConIngresoCeroOmiteBalance() {
        val sinIngreso = ExportarContenido(
            nombreArchivo = contenido.nombreArchivo,
            hojas = listOf(HojaExcel(nombre = "Sep 2026", gastos = gastos, ingresoTotal = 0.0))
        )
        val s = XlsxGenerador.generar(sinIngreso).aTexto()

        assertTrue(!s.contains("c r=\"G2\""), "no debe existir celda G2 sin ingreso")
        assertTrue(!s.contains("c r=\"I2\""), "no debe existir celda I2 sin ingreso")
    }

    @Test
    fun libroMultiHojaCreaUnaSheetPorMes() {
        val sep = HojaExcel("Sep 2026", gastos, 3565.0)
        val oct = HojaExcel("Oct 2026", listOf(gastos.first()), 0.0)
        val multi = ExportarContenido("Gastos.xlsx", listOf(sep, oct))
        val s = XlsxGenerador.generar(multi).aTexto()

        assertTrue(s.contains("<sheet name=\"Sep 2026\" sheetId=\"1\" r:id=\"rId1\"/>"), "falta hoja sep")
        assertTrue(s.contains("<sheet name=\"Oct 2026\" sheetId=\"2\" r:id=\"rId2\"/>"), "falta hoja oct")
        assertTrue(s.contains("xl/worksheets/sheet1.xml"), "falta la rel de hoja 1")
        assertTrue(s.contains("xl/worksheets/sheet2.xml"), "falta la rel de hoja 2")
        assertEquals(1, "c r=\"G2\"".toRegex().findAll(s).count(), "solo la hoja con ingreso lleva G2")
    }

    @Test
    fun libroMultiHojaUbicaLasHojasBajoXlWorksheets() {
        val sep = HojaExcel("Sep 2026", gastos, 3565.0)
        val oct = HojaExcel("Oct 2026", listOf(gastos.first()), 0.0)
        val multi = ExportarContenido("Gastos.xlsx", listOf(sep, oct))
        val nombres = XlsxGenerador.generar(multi).nombresZip()

        assertTrue("xl/worksheets/sheet1.xml" in nombres, "sheet1 debe existir como parte en xl/worksheets/ (rel del workbook)")
        assertTrue("xl/worksheets/sheet2.xml" in nombres, "sheet2 debe existir como parte en xl/worksheets/")
        assertTrue(!("sheet1.xml" in nombres), "no debe haber hojas colgadas en la raíz del zip")
    }

    @Test
    fun libroMultiHojaComparteSharedStrings() {
        val sep = HojaExcel("Sep 2026", gastos, 3565.0)
        val oct = HojaExcel("Oct 2026", gastos, 3565.0)
        val multi = ExportarContenido("Gastos.xlsx", listOf(sep, oct))
        val s = XlsxGenerador.generar(multi).aTexto()

        assertEquals(1, "<sst ".toRegex().findAll(s).count(), "debe haber un único sharedStrings")
        assertEquals(1, "Luz".toRegex().findAll(s).count(), "cadenas compartidas sin duplicar por hoja")
    }
}
