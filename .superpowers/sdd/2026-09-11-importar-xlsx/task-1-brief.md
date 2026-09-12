### Task 1: Núcleo lector (modelos + ZipReader + ImportarXlsx)

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/angel/gg/domain/model/Importacion.kt`
- Create: `composeApp/src/commonMain/kotlin/com/angel/gg/export/ZipReader.kt`
- Create: `composeApp/src/commonMain/kotlin/com/angel/gg/export/ImportarXlsx.kt`
- Test: `composeApp/src/commonTest/kotlin/com/angel/gg/export/ImportarXlsxTest.kt`

**Interfaces:**
- Consumes: `ZipWriter.zip(List<Pair<String, ByteArray>>)` (existente), `IngresoMensual(id: String, anio: Int, mes: Int, monto: Double)` (existente), `java.time.LocalDate`.
- Produces:
  - `domain/model/Importacion.kt`: `FilaImportada(monto, descripcion, categoriaNombre, fecha: LocalDate, esCuota)`, `ResultadoImportacion(filas: List<FilaImportada>, ingresosPorMes: List<IngresoMensual>)`, `ResumenImportacion(nuevos, omitidos, categoriasCreadas, ingresosRestaurados: Int)`, `sealed interface ImportacionResultado { data class Exito(val resumen: ResumenImportacion); data object SinGastos }`.
  - `ZipReader.leer(bytes): List<Pair<String, ByteArray>>`; throws `ZipReader.NoEsZip` (data class de Exception) si no hay ninguna entrada local.
  - `ImportarXlsx.leer(bytes): ResultadoLectura`; `sealed interface ResultadoLectura { data class Ok(val datos: ResultadoImportacion); data object ArchivoInvalido }`.

- [ ] **Step 1: Write the failing test**

`ImportarXlsxTest.kt` completo:

```kotlin
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
}
```

- [ ] **Step 2: Run test to verify it fails**

Run (workdir `D:\GestorGastos`): `.\gradlew.bat :composeApp:jvmTest --tests "com.angel.gg.export.ImportarXlsxTest"`
Expected: FAIL — `ImportarXlsx` / `ZipReader` / `FilaImportada` no definidos.

- [ ] **Step 3: Create `domain/model/Importacion.kt`**

```kotlin
package com.angel.gg.domain.model

import java.time.LocalDate

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

- [ ] **Step 4: Create `export/ZipReader.kt`**

```kotlin
package com.angel.gg.export

object ZipReader {

    data class NoEsZip(val motivo: String) : Exception(motivo)

    fun leer(bytes: ByteArray): List<Pair<String, ByteArray>> {
        val entradas = ArrayList<Pair<String, ByteArray>>()
        var pos = 0
        var leidas = 0
        while (pos + 30 <= bytes.size) {
            if (u32(bytes, pos) != 0x04034b50L) break
            val fnameLen = u16(bytes, pos + 26)
            val extraLen = u16(bytes, pos + 28)
            val compSize = u32(bytes, pos + 18).toInt()
            val dataStart = pos + 30 + fnameLen + extraLen
            val nombre = bytes.copyOfRange(pos + 30, pos + 30 + fnameLen).toString(Charsets.UTF_8)
            entradas.add(nombre to bytes.copyOfRange(dataStart, dataStart + compSize))
            pos = dataStart + compSize
            leidas++
        }
        if (leidas == 0) throw NoEsZip("no se encontraron entradas")
        return entradas
    }

    private fun u16(b: ByteArray, o: Int): Int =
        (b[o].toInt() and 0xFF) or ((b[o + 1].toInt() and 0xFF) shl 8)

    private fun u32(b: ByteArray, o: Int): Long =
        (b[o].toLong() and 0xFFL) or
        ((b[o + 1].toLong() and 0xFFL) shl 8) or
        ((b[o + 2].toLong() and 0xFFL) shl 16) or
        ((b[o + 3].toLong() and 0xFFL) shl 24)
}
```

- [ ] **Step 5: Create `export/ImportarXlsx.kt`**

```kotlin
package com.angel.gg.export

import com.angel.gg.domain.model.FilaImportada
import com.angel.gg.domain.model.IngresoMensual
import com.angel.gg.domain.model.ResultadoImportacion
import java.time.LocalDate

object ImportarXlsx {

    sealed interface ResultadoLectura {
        data class Ok(val datos: ResultadoImportacion) : ResultadoLectura
        data object ArchivoInvalido : ResultadoLectura
    }

    private val BASE_EXCEL = LocalDate.of(1899, 12, 30)

    fun leer(bytes: ByteArray): ResultadoLectura = runCatching {
        val porNombre = ZipReader.leer(bytes).toMap()
        val workbook = textoDe(porNombre, "xl/workbook.xml")
        val rels = textoDe(porNombre, "xl/_rels/workbook.xml.rels")
        val shared = sharedDe(textoDe(porNombre, "xl/sharedStrings.xml"))

        val filas = ArrayList<FilaImportada>()
        val ingresos = ArrayList<IngresoMensual>()

        hojasEnOrden(workbook, rels).forEach { archivo ->
            val xml = porNombre[archivo]?.toString(Charsets.UTF_8)
                ?: throw ZipReader.NoEsZip("falta parte $archivo")
            val hoja = hojaDe(xml, shared)
            filas += hoja.filas
            if (hoja.ingreso != null) ingresos += hoja.ingreso
        }

        ResultadoImportacion(filas = filas, ingresosPorMes = ingresos)
    }.getOrElse { ResultadoLectura.ArchivoInvalido }

    private fun textoDe(porNombre: Map<String, ByteArray>, nombre: String): String =
        porNombre[nombre]?.toString(Charsets.UTF_8)
            ?: throw ZipReader.NoEsZip("falta parte $nombre")

    private fun sharedDe(xml: String): List<String> {
        val lista = mutableListOf<String>()
        var from = 0
        while (true) {
            val ini = xml.indexOf("<si>", from)
            if (ini == -1) break
            val fin = xml.indexOf("</si>", ini)
            if (fin == -1) throw ZipReader.NoEsZip("sharedStrings mal formado")
            val bloque = xml.substring(ini, fin)
            val iniT = bloque.indexOf("<t>")
            lista.add(
                if (iniT == -1) "" else bloque.substring(iniT + 3, bloque.indexOf("</t>", iniT)).unescapa()
            )
            from = fin + 5
        }
        return lista
    }

    private fun hojasEnOrden(workbook: String, rels: String): List<String> {
        val idPorOrden = ArrayList<String>()
        var from = 0
        while (true) {
            val ini = workbook.indexOf("<sheet ", from)
            if (ini == -1) break
            val fin = workbook.indexOf('>', ini)
            if (fin == -1) break
            idPorOrden.add(atributo(workbook.substring(ini, fin), "r:id") ?: "")
            from = fin + 1
        }

        val targetPorId = HashMap<String, String>()
        from = 0
        while (true) {
            val ini = rels.indexOf("<Relationship ", from)
            if (ini == -1) break
            val fin = rels.indexOf('>', ini)
            if (fin == -1) break
            val bloque = rels.substring(ini, fin)
            if ((atributo(bloque, "Type") ?: "").endsWith("/worksheet")) {
                val id = atributo(bloque, "Id")
                val target = atributo(bloque, "Target")
                if (id != null && target != null) targetPorId[id] = "xl/$target"
            }
            from = fin + 1
        }

        return idPorOrden.map {
            targetPorId[it] ?: throw ZipReader.NoEsZip("hoja sin target")
        }
    }

    private fun atributo(bloque: String, nombre: String): String? {
        val ini = bloque.indexOf("$nombre=\"")
        if (ini == -1) return null
        val comienzo = ini + nombre.length + 2
        val fin = bloque.indexOf('"', comienzo)
        return if (fin == -1) null else bloque.substring(comienzo, fin)
    }

    private data class HojaParseada(
        val filas: List<FilaImportada>,
        val ingreso: IngresoMensual?
    )

    private fun hojaDe(xml: String, shared: List<String>): HojaParseada {
        val filas = ArrayList<FilaImportada>()
        var from = 0
        while (true) {
            val ini = xml.indexOf("<row ", from)
            if (ini == -1) break
            val fin = xml.indexOf("</row>", ini)
            if (fin == -1) throw ZipReader.NoEsZip("fila sin cierre")
            val bloque = xml.substring(ini, fin)
            val r = atributo(bloque, "r")?.toIntOrNull() ?: 0
            if (r > 1) {
                val monto = valorCelda(bloque, "A$r")?.toDoubleOrNull()
                val descIdx = valorCelda(bloque, "B$r")?.toIntOrNull()
                val catIdx = valorCelda(bloque, "C$r")?.toIntOrNull()
                val fecha = valorCelda(bloque, "D$r")?.let { v ->
                    v.toDoubleOrNull()?.let { BASE_EXCEL.plusDays(it.toLong()) } ?: LocalDate.parse(v)
                }
                if (monto != null && descIdx != null && catIdx != null && fecha != null) {
                    filas.add(
                        FilaImportada(
                            monto = monto,
                            descripcion = shared.getOrNull(descIdx) ?: "",
                            categoriaNombre = shared.getOrNull(catIdx) ?: "",
                            fecha = fecha,
                            esCuota = valorCelda(bloque, "E$r") != null
                        )
                    )
                }
            }
            from = fin + 6
        }

        val ingreso = valorCelda(xml, "G2")?.toDoubleOrNull()?.let { monto ->
            val (anio, mes) = mesDe(filas)
            if (anio != null && mes != null) {
                IngresoMensual(id = "", anio = anio, mes = mes, monto = monto)
            } else null
        }
        return HojaParseada(filas, ingreso)
    }

    private fun mesDe(filas: List<FilaImportada>): Pair<Int?, Int?> {
        if (filas.isEmpty()) return null to null
        return filas.first().fecha.year to filas.first().fecha.monthValue
    }

    private fun valorCelda(bloque: String, ref: String): String? {
        val ini = bloque.indexOf("<c r=\"$ref\"")
        if (ini == -1) return null
        val finTag = bloque.indexOf('>', ini)
        if (bloque.getOrNull(finTag - 1) == '/') return null
        val iniV = bloque.indexOf("<v>", finTag)
        if (iniV == -1) return null
        val finV = bloque.indexOf("</v>", iniV)
        if (finV == -1) return null
        return bloque.substring(iniV + 3, finV).unescapa()
    }

    private fun String.unescapa(): String =
        replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">")
            .replace("&quot;", "\"").replace("&apos;", "'")
}
```

- [ ] **Step 6: Run test to verify it passes**

Run (workdir `D:\GestorGastos`): `.\gradlew.bat :composeApp:jvmTest --tests "com.angel.gg.export.ImportarXlsxTest"`
Expected: PASS (5 tests). También correr la suite completa: `.\gradlew.bat :composeApp:jvmTest`.

- [ ] **Step 7: Gates**

Run (workdir `D:\GestorGastos`): `.\gradlew.bat :composeApp:jvmTest :composeApp:compileKotlinJvm :composeApp:compileDebugKotlinAndroid`
Expected: tests PASS (13 = 8 del generador + 5 del lector) y BUILD SUCCESSFUL.

---

