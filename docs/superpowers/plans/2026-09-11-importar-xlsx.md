# Importación de gastos desde .xlsx — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Permitir en Ajustes → Importar seleccionar un `.xlsx` exportado por la app y cargar sus gastos (y los ingresos mensuales por hoja) en la base, salteando duplicados y recreando categorías faltantes.

**Architecture:** Espejo de la arquitectura de exportación. El núcleo (`ZipReader` + `ImportarXlsx` + `ImportarGastosUseCase`) vive en commonMain y es puro/testeable con TDD; la lectura de archivo es `expect/actual` (`ImportarLectura`); la UI es un `ImportarViewModel` + `ImportarDialog` conectados en `AjustesScreen`, con DI en `AppModule`.

**Tech Stack:** Kotlin Multiplatform / Compose Multiplatform, SQLDelight, Koin, material3; sin dependencias nuevas. `java.time` disponible en commonMain.

**Spec:** `docs/superpowers/specs/2026-09-11-importar-xlsx-design.md`

## Global Constraints

- El proyecto NO es un repositorio git (`D:\GestorGastos`) — **no hay commits**; cada tarea verifica con gates.
- Gates por tarea: `.\gradlew.bat :composeApp:jvmTest` (PASS) y `.\gradlew.bat :composeApp:compileKotlinJvm :composeApp:compileDebugKotlinAndroid` (BUILD SUCCESSFUL).
- No agregar dependencias nuevas.
- Seguir convenciones existentes: `AppTheme.*`, Material3, `Strings.t(clave, vararg args)` / `tr(...)` con placeholders `{0}`, `{1}`…, `Strings.langNow()`.
- Reusar `generarId()` (usecases/FechaUtils.kt) y `GestionarCategoriaUseCase` existentes. `ImportarXlsx` es un `object` (como `XlsxGenerador`); `ZipWriter`/`XlsxGenerador` NO se modifican.
- Claves i18n nuevas SOLO `importar.*` (ES/EN/FR). `ajustes.importar` / `ajustes.importarSub` ya existen: no tocar.

---

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

        ResultadoLectura.Ok(ResultadoImportacion(filas = filas, ingresosPorMes = ingresos))
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

### Task 2: Use case de importación

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/angel/gg/domain/usecase/ImportarGastosUseCase.kt`
- Test: `composeApp/src/commonTest/kotlin/com/angel/gg/domain/usecase/ImportarGastosUseCaseTest.kt`

**Interfaces:**
- Consumes (de Task 1): `ResultadoImportacion`, `ImportacionResultado`/`ResumenImportacion`, `FilaImportada`. Existentes: `Gasto(id, monto, descripcion, categoriaId, categoriaNombre, categoriaColor, categoriaIcono, fecha: String ISO, anio, mes, cuotaActual? = null, cuotaTotal? = null, idPadre? = null)`, `Categoria(id, nombre, descripcion?, color, icono, orden, fijada)`, `IngresoMensual(id, anio, mes, monto)`, `GastoRepository.obtenerPorRango(desde: String, hasta: String)` e `insertar(gasto)`, `CategoriaRepository.obtenerTodas(): Flow<List<Categoria>>`, `IngresoMensualRepository.guardar(ingreso)`, `GestionarCategoriaUseCase.crear(nombre, descripcion?, color, icono, fijada): Resultado { Exito(categoria) | Fallo(...) }`, `generarId()`.
- Produces: `class ImportarGastosUseCase(gastoRepository, categoriaRepository, ingresoMensualRepository, gestionarCategoriaUseCase)` con `suspend operator fun invoke(datos: ResultadoImportacion): ImportacionResultado`.

- [ ] **Step 1: Write the failing test**

`ImportarGastosUseCaseTest.kt` completo:

```kotlin
package com.angel.gg.domain.usecase

import com.angel.gg.domain.model.Categoria
import com.angel.gg.domain.model.FilaImportada
import com.angel.gg.domain.model.Gasto
import com.angel.gg.domain.model.IngresoMensual
import com.angel.gg.domain.model.ImportacionResultado
import com.angel.gg.domain.model.ResultadoImportacion
import com.angel.gg.domain.repository.CategoriaRepository
import com.angel.gg.domain.repository.GastoRepository
import com.angel.gg.domain.repository.IngresoMensualRepository
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ImportarGastosUseCaseTest {

    private class RepoGastosFake : GastoRepository {
        val datos = mutableListOf<Gasto>()
        override fun obtenerPorMes(anio: Int, mes: Int): Flow<List<Gasto>> =
            flowOf(datos.filter { it.anio == anio && it.mes == mes })
        override fun obtenerGastosPorCategoriaMes(anio: Int, mes: Int): Flow<List<GastoCategoriaDb>> =
            flowOf(emptyList())
        override suspend fun obtenerPorId(id: String): Gasto? = datos.find { it.id == id }
        override suspend fun obtenerPorIdPadre(idPadre: String): List<Gasto> = datos.filter { it.idPadre == idPadre }
        override suspend fun buscarPorDescripcion(query: String): List<Gasto> = datos.filter { it.descripcion.contains(query) }
        override suspend fun totalPorMes(anio: Int, mes: Int): Double = 0.0
        override suspend fun obtenerPorRango(desde: String, hasta: String): List<Gasto> =
            datos.filter { it.fecha >= desde && it.fecha <= hasta }
        override suspend fun insertar(gasto: Gasto) { datos.add(gasto) }
        override suspend fun insertarSerieCuotas(gastos: List<Gasto>) { datos.addAll(gastos) }
        override suspend fun actualizarUno(gasto: Gasto) {
            val i = datos.indexOfFirst { it.id == gasto.id }
            if (i != -1) datos[i] = gasto
        }
        override suspend fun actualizarCuotasFuturas(idPadre: String, desdeCuota: Int, monto: Double, descripcion: String, categoriaId: String) {}
        override suspend fun actualizarTodaSerie(idPadre: String, monto: Double, descripcion: String, categoriaId: String) {}
        override suspend fun eliminarUno(id: String) { datos.removeAll { it.id == id } }
        override suspend fun eliminarCuotasFuturas(idPadre: String, desdeCuota: Int) {}
        override suspend fun eliminarTodaSerie(idPadre: String) {}
        override suspend fun contarGastosPorCategoria(categoriaId: String): Int = datos.count { it.categoriaId == categoriaId }
    }

    private class RepoCategoriasFake : CategoriaRepository {
        val datos = mutableListOf<Categoria>()
        override fun obtenerTodas(): Flow<List<Categoria>> = flowOf(datos.toList())
        override suspend fun obtenerPorId(id: String): Categoria? = datos.find { it.id == id }
        override suspend fun insertar(categoria: Categoria) { datos.add(categoria) }
        override suspend fun actualizar(categoria: Categoria) {}
        override suspend fun actualizarOrden(id: String, orden: Int) {}
        override suspend fun eliminar(id: String) { datos.removeAll { it.id == id } }
    }

    private class RepoIngresosFake : IngresoMensualRepository {
        val datos = mutableListOf<IngresoMensual>()
        override fun obtenerPorMes(anio: Int, mes: Int): Flow<IngresoMensual?> =
            flowOf(datos.find { it.anio == anio && it.mes == mes })
        override suspend fun guardar(ingresoMensual: IngresoMensual) {
            datos.removeAll { it.anio == ingresoMensual.anio && it.mes == ingresoMensual.mes }
            datos.add(ingresoMensual)
        }
        override suspend fun obtenerEnRango(desdeKey: Long, hastaKey: Long): List<IngresoMensual> =
            datos.filter { (it.anio * 100L + it.mes) in desdeKey..hastaKey }
    }

    private val sep2026 = LocalDate.of(2026, 9, 1)

    private data class Caso(
        val useCase: ImportarGastosUseCase,
        val gastos: RepoGastosFake,
        val cats: RepoCategoriasFake,
        val ingresos: RepoIngresosFake
    )

    private fun caso(): Caso {
        val gastos = RepoGastosFake()
        val cats = RepoCategoriasFake()
        val ingresos = RepoIngresosFake()
        val gestionar = GestionarCategoriaUseCase(cats, gastos)
        val useCase = ImportarGastosUseCase(gastos, cats, ingresos, gestionar)
        return Caso(useCase, gastos, cats, ingresos)
    }

    private val caso = caso()

    @Test
    fun importaFilasNuevasYCuentaResumen() {
        val useCase = caso.useCase
        val datos = ResultadoImportacion(
            filas = listOf(
                FilaImportada(100.0, "Pago mensual", "Luz", sep2026, esCuota = false),
                FilaImportada(222.0, "Depto", "Departamento", LocalDate.of(2026, 9, 5), esCuota = false)
            ),
            ingresosPorMes = emptyList()
        )
        val r = assertIs<ImportacionResultado.Exito>(runBlocking { useCase(datos) })
        assertEquals(2, r.resumen.nuevos)
        assertEquals(0, r.resumen.omitidos)
        assertEquals(2, r.resumen.categoriasCreadas)
        assertEquals(0, r.resumen.ingresosRestaurados)
        assertEquals(2, caso.gastos.datos.size)
    }

    @Test
    fun omiteDuplicados() {
        caso.gastos.datos.add(
            Gasto(
                id = "g1", monto = 100.0, descripcion = "pago mensual ", categoriaId = "c-luz",
                categoriaNombre = "luz", categoriaColor = "#4285F4", categoriaIcono = "",
                fecha = "2026-09-01", anio = 2026, mes = 9
            )
        )
        val useCase = caso.useCase
        val datos = ResultadoImportacion(
            filas = listOf(
                FilaImportada(100.0, "Pago mensual", "Luz", sep2026, esCuota = false),
                FilaImportada(50.0, "Taxi", "Transporte", LocalDate.of(2026, 9, 2), esCuota = false)
            ),
            ingresosPorMes = emptyList()
        )
        val r = assertIs<ImportacionResultado.Exito>(runBlocking { useCase(datos) })
        assertEquals(1, r.resumen.nuevos)
        assertEquals(1, r.resumen.omitidos)
    }

    @Test
    fun recreaCategoriaUnaSolaVez() {
        val useCase = caso.useCase
        val datos = ResultadoImportacion(
            filas = listOf(
                FilaImportada(10.0, "Mercado", "Alimentación", sep2026, esCuota = false),
                FilaImportada(20.0, "Café", "Alimentación", LocalDate.of(2026, 9, 2), esCuota = false),
                FilaImportada(30.0, "Pan", "Alimentación", LocalDate.of(2026, 9, 3), esCuota = false)
            ),
            ingresosPorMes = emptyList()
        )
        val r = assertIs<ImportacionResultado.Exito>(runBlocking { useCase(datos) })
        assertEquals(1, r.resumen.categoriasCreadas)
        assertEquals(1, caso.cats.datos.size)
        val nombreCategoriaRepo = caso.cats.datos.single().nombre
        caso.gastos.datos.forEach { assertEquals(nombreCategoriaRepo, it.categoriaNombre) }
        assertEquals(1, caso.gastos.datos.map { it.categoriaId }.distinct().size)
    }

    @Test
    fun restauraIngresosPorMes() {
        val useCase = caso.useCase
        val datos = ResultadoImportacion(
            filas = listOf(FilaImportada(10.0, "Mercado", "Alimentación", sep2026, esCuota = false)),
            ingresosPorMes = listOf(IngresoMensual(id = "", anio = 2026, mes = 9, monto = 3565.0))
        )
        val r = assertIs<ImportacionResultado.Exito>(runBlocking { useCase(datos) })
        assertEquals(1, r.resumen.ingresosRestaurados)
        assertEquals(1, caso.ingresos.datos.size)
        assertEquals(3565.0, caso.ingresos.datos.single().monto)
    }

    @Test
    fun sinFilasDevuelveSinGastos() {
        val useCase = caso.useCase
        val datos = ResultadoImportacion(filas = emptyList(), ingresosPorMes = emptyList())
        assertIs<ImportacionResultado.SinGastos>(runBlocking { useCase(datos) })
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run (workdir `D:\GestorGastos`): `.\gradlew.bat :composeApp:jvmTest --tests "com.angel.gg.domain.usecase.ImportarGastosUseCaseTest"`
Expected: FAIL — `ImportarGastosUseCase` no definido.

- [ ] **Step 3: Create `domain/usecase/ImportarGastosUseCase.kt`**

```kotlin
package com.angel.gg.domain.usecase

import com.angel.gg.domain.model.Categoria
import com.angel.gg.domain.model.Gasto
import com.angel.gg.domain.model.IngresoMensual
import com.angel.gg.domain.model.ImportacionResultado
import com.angel.gg.domain.model.ResultadoImportacion
import com.angel.gg.domain.model.ResumenImportacion
import com.angel.gg.domain.repository.CategoriaRepository
import com.angel.gg.domain.repository.GastoRepository
import com.angel.gg.domain.repository.IngresoMensualRepository
import kotlinx.coroutines.flow.first

class ImportarGastosUseCase(
    private val gastoRepository: GastoRepository,
    private val categoriaRepository: CategoriaRepository,
    private val ingresoMensualRepository: IngresoMensualRepository,
    private val gestionarCategoriaUseCase: GestionarCategoriaUseCase
) {
    private data class DedupKey(
        val monto: Double,
        val descripcion: String,
        val categoriaNombre: String,
        val fecha: String
    ) {
        companion object {
            fun de(gasto: Gasto) =
                DedupKey(gasto.monto, gasto.descripcion.trim().lowercase(), gasto.categoriaNombre.trim().lowercase(), gasto.fecha)
            fun deFila(monto: Double, descripcion: String, categoriaNombre: String, fecha: String) =
                DedupKey(monto, descripcion.trim().lowercase(), categoriaNombre.trim().lowercase(), fecha)
        }
    }

    suspend operator fun invoke(datos: ResultadoImportacion): ImportacionResultado {
        if (datos.filas.isEmpty()) return ImportacionResultado.SinGastos

        val desde = datos.filas.minOf { it.fecha }.toString()
        val hasta = datos.filas.maxOf { it.fecha }.toString()
        val existentes = HashSet<DedupKey>()
        gastoRepository.obtenerPorRango(desde, hasta).forEach { existentes.add(DedupKey.de(it)) }

        val categorias = HashMap<String, Categoria>()
        categoriaRepository.obtenerTodas().first().forEach {
            categorias[it.nombre.trim().lowercase()] = it
        }
        var creadas = 0

        suspend fun resolver(nombre: String): Categoria {
            val clave = nombre.trim().lowercase()
            categorias[clave]?.let { return it }
            val resultado = gestionarCategoriaUseCase.crear(nombre, null, "#4F46E5", "default", false)
            val categoria = (resultado as? GestionarCategoriaUseCase.Resultado.Exito)?.categoria
                ?: throw IllegalStateException("no se pudo crear la categoría $nombre")
            categorias[clave] = categoria
            creadas++
            return categoria
        }

        var nuevos = 0
        var omitidos = 0
        for (fila in datos.filas) {
            val clave = DedupKey.deFila(fila.monto, fila.descripcion, fila.categoriaNombre, fila.fecha.toString())
            if (clave in existentes) {
                omitidos++
                continue
            }
            val categoria = resolver(fila.categoriaNombre)
            gastoRepository.insertar(
                Gasto(
                    id = generarId(),
                    monto = fila.monto,
                    descripcion = fila.descripcion.trim(),
                    categoriaId = categoria.id,
                    categoriaNombre = categoria.nombre,
                    categoriaColor = categoria.color,
                    categoriaIcono = categoria.icono,
                    fecha = fila.fecha.toString(),
                    anio = fila.fecha.year,
                    mes = fila.fecha.monthValue
                )
            )
            existentes.add(clave)
            nuevos++
        }

        var ingresosRestaurados = 0
        for (ingreso in datos.ingresosPorMes) {
            if (ingreso.monto > 0) {
                ingresoMensualRepository.guardar(IngresoMensual(generarId(), ingreso.anio, ingreso.mes, ingreso.monto))
                ingresosRestaurados++
            }
        }

        return ImportacionResultado.Exito(
            ResumenImportacion(nuevos, omitidos, creadas, ingresosRestaurados)
        )
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run (workdir `D:\GestorGastos`): `.\gradlew.bat :composeApp:jvmTest --tests "com.angel.gg.domain.usecase.ImportarGastosUseCaseTest"`
Expected: PASS (5 tests).

- [ ] **Step 5: Gates**

Run (workdir `D:\GestorGastos`): `.\gradlew.bat :composeApp:jvmTest :composeApp:compileKotlinJvm :composeApp:compileDebugKotlinAndroid`
Expected: tests PASS y BUILD SUCCESSFUL.

---

### Task 3: Lectura de archivo (expect/actual)

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/angel/gg/export/ImportarLectura.kt`
- Create: `composeApp/src/jvmMain/kotlin/com/angel/gg/export/ImportarLectura.jvm.kt`
- Create: `composeApp/src/androidMain/kotlin/com/angel/gg/export/ImportarLectura.android.kt`
- Modify: `composeApp/src/androidMain/kotlin/com/angel/gg/MainActivity.kt`

**Interfaces:**
- Consumes: patrones de `ExportarGuardado` (`expect class` + actuals, puente SAF en Android).
- Produces: `expect class ImportarLectura { suspend fun leer(): ByteArray? }` — `null` si el usuario cancela.

- [ ] **Step 1: Create commonMain expect**

`ImportarLectura.kt`:

```kotlin
package com.angel.gg.export

expect class ImportarLectura {
    suspend fun leer(): ByteArray?
}
```

- [ ] **Step 2: Create jvmMain actual**

`ImportarLectura.jvm.kt`:

```kotlin
package com.angel.gg.export

import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.swing.Swing

actual class ImportarLectura {
    actual suspend fun leer(): ByteArray? =
        withContext(Dispatchers.Swing) {
            val chooser = JFileChooser().apply {
                dialogTitle = "Importar xlsx"
                fileFilter = FileNameExtensionFilter("Excel (.xlsx)", "xlsx")
            }
            if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                runCatching { chooser.selectedFile.readBytes() }.getOrNull()
            } else null
        }
}
```

- [ ] **Step 3: Create androidMain actual**

`ImportarLectura.android.kt`:

```kotlin
package com.angel.gg.export

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Puente entre el launcher de SAF (registrado en MainActivity) y el suspend leer
object ImportarPuente {
    private var launcher: (() -> Unit)? = null
    private var contexto: Context? = null
    private var pendiente: CompletableDeferred<Uri?>? = null

    fun registrar(lanzar: () -> Unit, ctx: Context) {
        launcher = lanzar
        contexto = ctx
    }

    fun resolver(uri: Uri?) {
        pendiente?.complete(uri)
        pendiente = null
    }

    suspend fun leer(): ByteArray? {
        launcher ?: return null
        val d = CompletableDeferred<Uri?>()
        pendiente = d
        launcher?.invoke()
        val uri = d.await() ?: return null
        val ctx = contexto ?: return null
        return withContext(Dispatchers.IO) {
            runCatching { ctx.contentResolver.openInputStream(uri)?.use { it.readBytes() } }.getOrNull()
        }
    }
}

actual class ImportarLectura {
    actual suspend fun leer(): ByteArray? = ImportarPuente.leer()
}
```

- [ ] **Step 4: Registro del launcher en MainActivity (androidMain)**

`MainActivity.kt` — añadir junto al bloque `ExportarPuente` existente (mantenerlo intacto). Añadir el import con los demás `com.angel.gg.*` (tras línea 10), el campo junto a `crearDocumento` (tras línea 19), y el registro en `onCreate` justo después de `ExportarPuente.registrar(...)` (línea 23):

```kotlin
import com.angel.gg.export.ImportarPuente
...
    private val abrirDocumento = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> ImportarPuente.resolver(uri) }
...
        ImportarPuente.registrar(
            { abrirDocumento.launch(arrayOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) },
            applicationContext
        )
```

- [ ] **Step 5: Gates**

Run (workdir `D:\GestorGastos`): `.\gradlew.bat :composeApp:jvmTest :composeApp:compileKotlinJvm :composeApp:compileDebugKotlinAndroid`
Expected: tests PASS y BUILD SUCCESSFUL (incluye Android con el nuevo launcher).

---

### Task 4: UI + i18n + DI

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/angel/gg/presentation/viewmodel/ImportarViewModel.kt`
- Create: `composeApp/src/commonMain/kotlin/com/angel/gg/presentation/screen/ImportarDialog.kt`
- Modify: `composeApp/src/androidMain/kotlin/com/angel/gg/di/AndroidModule.kt`
- Modify: `composeApp/src/jvmMain/kotlin/com/angel/gg/di/DesktopModule.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/angel/gg/di/AppModule.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/angel/gg/presentation/i18n/EsStrings.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/angel/gg/presentation/i18n/EnStrings.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/angel/gg/presentation/i18n/FrStrings.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/angel/gg/presentation/screen/AjustesScreen.kt`

**Interfaces:**
- Consumes (Tasks 1–3): `ImportarXlsx.leer(bytes): ResultadoLectura { Ok(datos: ResultadoImportacion) | ArchivoInvalido }`, `ImportarLectura.leer(): ByteArray?`, `ImportarGastosUseCase(datos): ImportacionResultado { Exito(resumen) | SinGastos }`. Existentes: `Strings.t(key, vararg args)` con placeholders `{0}`…, `tr(key)`, `AppTheme.*`, `AppIcons.Importar`, `koinInject()`.
- Produces: `class ImportarViewModel(importarLectura, importarGastosUseCase)` con `state: StateFlow<ImportarUiState>` y `importar()`/`limpiarMensaje()`; `ImportarDialog(onDismiss, viewModel = koinInject())`.

- [ ] **Step 1: i18n — añadir claves `importar.*` (ES/EN/FR)**

En `EsStrings.kt`, justo después de la entrada `"exportar.sinDatos"` (mantener el bloque `exportar.*` completo):

```kotlin
        "importar.titulo"          to "Importar gastos",
        "importar.boton"           to "Seleccionar archivo",
        "importar.exito"           to "Se importaron {0} gastos ({1} ya existían, {2} categorías creadas, {3} ingresos)",
        "importar.error"           to "No se pudo importar",
        "importar.archivoNoValido" to "El archivo no es un xlsx válido",
        "importar.sinGastos"       to "El archivo no contiene gastos",
```

En `EnStrings.kt`, análogo (guardar el bloque `exportar.*` intacto):

```kotlin
        "importar.titulo"          to "Import expenses",
        "importar.boton"           to "Select file",
        "importar.exito"           to "Imported {0} expenses ({1} already existed, {2} categories created, {3} incomes)",
        "importar.error"           to "Import failed",
        "importar.archivoNoValido" to "The file is not a valid xlsx",
        "importar.sinGastos"       to "The file contains no expenses",
```

En `FrStrings.kt`, análogo (guardar el bloque `exportar.*` intacto):

```kotlin
        "importar.titulo"          to "Importer des dépenses",
        "importar.boton"           to "Choisir un fichier",
        "importar.exito"           to "{0} dépenses importées ({1} existaient déjà, {2} catégories créées, {3} revenus)",
        "importar.error"           to "Échec de l'importation",
        "importar.archivoNoValido" to "Le fichier n'est pas un xlsx valide",
        "importar.sinGastos"       to "Le fichier ne contient aucune dépense",
```

- [ ] **Step 2: Create `presentation/viewmodel/ImportarViewModel.kt`**

```kotlin
package com.angel.gg.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.angel.gg.domain.model.ImportacionResultado
import com.angel.gg.domain.usecase.ImportarGastosUseCase
import com.angel.gg.export.ImportarLectura
import com.angel.gg.export.ImportarXlsx
import com.angel.gg.presentation.i18n.Strings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ImportarUiState(
    val importando: Boolean = false,
    val mensaje: String? = null,
    val esError: Boolean = false
)

class ImportarViewModel(
    private val importarLectura: ImportarLectura,
    private val importarGastosUseCase: ImportarGastosUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(ImportarUiState())
    val state: StateFlow<ImportarUiState> = _state

    fun importar() {
        viewModelScope.launch {
            _state.update { it.copy(importando = true, mensaje = null) }
            runCatching {
                val bytes = importarLectura.leer() ?: run {
                    _state.update { it.copy(importando = false) }
                    return@launch
                }
                when (val lectura = ImportarXlsx.leer(bytes)) {
                    is ImportarXlsx.ResultadoLectura.ArchivoInvalido ->
                        _state.update {
                            it.copy(importando = false, mensaje = Strings.t("importar.archivoNoValido"), esError = true)
                        }
                    is ImportarXlsx.ResultadoLectura.Ok ->
                        when (val resultado = importarGastosUseCase(lectura.datos)) {
                            is ImportacionResultado.SinGastos ->
                                _state.update {
                                    it.copy(importando = false, mensaje = Strings.t("importar.sinGastos"), esError = true)
                                }
                            is ImportacionResultado.Exito ->
                                _state.update {
                                    it.copy(
                                        importando = false,
                                        mensaje = Strings.t(
                                            "importar.exito",
                                            resultado.resumen.nuevos,
                                            resultado.resumen.omitidos,
                                            resultado.resumen.categoriasCreadas,
                                            resultado.resumen.ingresosRestaurados
                                        ),
                                        esError = false
                                    )
                                }
                        }
                }
            }.onFailure {
                _state.update { it.copy(importando = false, mensaje = Strings.t("importar.error"), esError = true) }
            }
        }
    }

    fun limpiarMensaje() {
        _state.update { it.copy(mensaje = null, esError = false) }
    }
}
```

- [ ] **Step 3: Create `presentation/screen/ImportarDialog.kt`**

```kotlin
package com.angel.gg.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.angel.gg.presentation.AppTheme
import com.angel.gg.presentation.i18n.tr
import com.angel.gg.presentation.viewmodel.ImportarViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportarDialog(
    onDismiss: () -> Unit,
    viewModel: ImportarViewModel = koinInject()
) {
    val estado by viewModel.state.collectAsState()

    // Cerrar el diálogo al confirmar la importación (la snackbar muestra el resumen)
    LaunchedEffect(estado.mensaje) {
        val m = estado.mensaje
        if (m != null && !estado.esError) onDismiss()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppTheme.CardDark,
        title = { Text(tr("importar.titulo"), color = AppTheme.TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(tr("ajustes.importarSub"), color = AppTheme.TextMuted, fontSize = 13.sp)
                if (estado.importando) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.padding(8.dp), color = AppTheme.Indigo)
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
                enabled = !estado.importando,
                onClick = { viewModel.importar() }
            ) {
                Text(tr("importar.boton"), color = if (estado.importando) AppTheme.TextSubtle else AppTheme.Indigo)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(tr("comun.cancelar"), color = AppTheme.TextMuted)
            }
        }
    )
}
```

- [ ] **Step 4: DI — módulos de plataforma + AppModule**

**`AndroidModule.kt`** (junto a las otras líneas `single`, conservar el resto):

```kotlin
import com.angel.gg.export.ImportarLectura
...
    single<ImportarLectura> { ImportarLectura() }
```

**`DesktopModule.kt`** (análogo):

```kotlin
import com.angel.gg.export.ImportarLectura
...
    single<ImportarLectura> { ImportarLectura() }
```

**`AppModule.kt`** — añadir imports (no eliminar los existentes):

```kotlin
import com.angel.gg.domain.usecase.ImportarGastosUseCase
import com.angel.gg.presentation.viewmodel.ImportarViewModel
```

En `domainModule`, después del bloque de `ExportarGastosUseCase` (líneas 124-129, antes del cierre en línea 130):

```kotlin
    factory {
        ImportarGastosUseCase(
            gastoRepository           = get(),
            categoriaRepository       = get(),
            ingresoMensualRepository  = get(),
            gestionarCategoriaUseCase = get()
        )
    }
```

En `viewModelModule`, después del bloque de `ExportarViewModel` (líneas 194-199, antes del cierre en línea 200):

```kotlin
    factory {
        ImportarViewModel(
            importarLectura       = get(),
            importarGastosUseCase = get()
        )
    }
```

- [ ] **Step 5: Wire AjustesScreen**

`AjustesScreen.kt` (mantener bloque de exportación intacto):
- Añadir import: `import com.angel.gg.presentation.viewmodel.ImportarViewModel`
- Añadir estado y VM junto a los del bloque de exportar (líneas 40-42):

```kotlin
    var abrirImportar by remember { mutableStateOf(false) }
    val importarViewModel: ImportarViewModel = koinInject()
    val importarEstado by importarViewModel.state.collectAsState()
```

- Añadir efecto de snackbar justo después del `LaunchedEffect(exportarEstado.mensaje)` (líneas 45-50):

```kotlin
    LaunchedEffect(importarEstado.mensaje) {
        importarEstado.mensaje?.let {
            snackbarHostState.showSnackbar(it)
            importarViewModel.limpiarMensaje()
        }
    }
```

- Conectar la fila (AjustesScreen.kt:162-165): cambiar `onClick = {}` por `onClick = { abrirImportar = true }`.
- Añadir el diálogo junto al de exportar (después del bloque `if (abrirExportar) {...}` de líneas 175-180, dentro del `Scaffold`):

```kotlin
        if (abrirImportar) {
            ImportarDialog(
                onDismiss = { abrirImportar = false },
                viewModel = importarViewModel
            )
        }
```

- [ ] **Step 6: Gates**

Run (workdir `D:\GestorGastos`): `.\gradlew.bat :composeApp:jvmTest :composeApp:compileKotlinJvm :composeApp:compileDebugKotlinAndroid`
Expected: tests PASS y BUILD SUCCESSFUL (sin warnings de imports sin usar).

- [ ] **Step 7: Verificación manual (desktop, solo si hay GUI; si no, dejarlo pendiente para el humano)**

Run (workdir `D:\GestorGastos`): `.\gradlew.bat :composeApp:run`
Ajustes → Importar → seleccionar un .xlsx **exportado** por la app → debe aparecer el resumen en la snackbar; los gastos deben totalizarse en sus meses; importar el archivo de nuevo debe omitir todo (0 nuevos). Archivo corrupto → mensaje "no es un xlsx válido".
## FIX POST-ENTREGA (verificacion manual 2026-09-11)

**Bug:** al importar un xlsx reguardado por Excel/LibreOffice (comprimido DEFLATE), la app mostro falsamente "el archivo no contiene gastos" (SinGastos).
**Causa raiz:** `ZipReader.leer` leia el data crudo asumiendo entradas STORED (metodo 0, como escribe ZipWriter) y nunca miraba el byte de metodo (offsets 8-9). Con metodo 8 (DEFLATE) cada entrada quedaba como binario comprimido sin inflar; "workbook.xml" llegaba como basura, sin `<sheet`, => 0 hojas => `Ok(filas=0)` => SinGastos. El mensaje era engañoso: el archivo SI tenia gastos.
**Evidencia:** archivo real `Gastos Sep 2026.xlsx` en Downloads (reguardado por Excel): metodo de la primera entrada = 8 (DEFLATE). Sonda con `ImportarXlsx.leer` real sobre sus bytes: Ok filas=0. Test 1 manual OK porque el archivo recien exportado por la app es STORED.
**Fix (aprobado: soportar DEFLATE):**
1. `commonMain/export/ZipInflador.kt`: `expect fun inflarDeflate(bytes: ByteArray): ByteArray`.
2. `jvmMain/export/ZipInflador.jvm.kt` y `androidMain/export/ZipInflador.android.kt`: `InflaterInputStream(..., Inflater(true))` (raw deflate; NO zlib wrapper). stdlib, sin deps nuevas.
3. `ZipReader.leer`: lee `metodo` (bytes 8-9); 0 => crudo, 8 => inflarDeflate, otro => `NoEsZip`. Guard de data-descriptor (bit 3 flag) con compSize<=0 => `NoEsZip`.
4. Test de regresion `commonTest/.../DeflateXlsxTest.kt`: fixture = base64 del archivo real reguardado por Excel; asserta que `leer` devuelve Ok con 3 filas (Cuota Celular/Cuotas/2026-09-10, cuota; Pago mensual/Departamento, no cuota) y 1 ingreso (1000000, 2026-09).
**Verificacion:** jvmTest 19/19 PASS; compileKotlinJvm + compileDebugKotlinAndroid BUILD SUCCESSFUL (solo warnings preexistentes; el "No cast needed" espureo del test limpiado).
**Nota:** el alcance del import se amplio de "solo round-trip del exportador (STORED)" a "cualquier xlsx con entradas STORED o DEFLATE"; actualizar `specs/2026-09-11-importar-xlsx-design.md` si se desea documentar. Mensaje "no contiene gastos" queda reservado a archivos legitimos sin filas.

## MEJORA POST-FIX ${date}: defaults de import (requerimiento usuario)

Comportamiento de ImportarXlsx.hojaDe:
- Fila con monto > 0 = gasto nuevo. Sin monto, monto 0 o negativo => SE IGNORA (igual que antes si faltaba monto).
- Descripcion faltante o en blanco => "-".
- Categoria faltante o en blanco => "-" (resolver la crea al importar, ya lo hacia GestionarCategoriaUseCase).
- Fecha faltante o en blanco o no parseable => primer dia del mes actual (LocalDate.now().withDayOfMonth(1)).
- Tests nuevos en ImportarXlsxTest (builder de filas crudas): 7 casos (guion desc, guion cat, fecha default, sin monto ignora, monto 0/negativo ignora, fecha en blanco, combinado). Gates: jvmTest 26/26 PASS, compiles SUCCESSFUL.
- Pendiente humano: re-testeo manual con el archivo reguardado por Excel (ya importa) incluidos gastos sin desc/cat/fecha, y verificar que la categoria "-" se crea y la fecha default es el 1 del mes actual.
