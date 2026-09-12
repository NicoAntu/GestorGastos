# Exportar a .xlsx — Hojas mensuales + fix calendario — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Extender la exportación a .xlsx para generar una hoja por mes (los 4 alcances) y arreglar el DatePicker del rango Personalizado que no se abre.

**Architecture:** El refactor toca el núcleo en una sola unidad atómica (modelo → use case → ViewModel → generador → tests) porque `ExportarContenido` cambia de forma; el fix de UI es independiente y va después. La agrupación por mes vive en `ExportarViewModel` (usa los `IngresoMensual` que ya consulta el use case) y `XlsxGenerador` aprende a emitir N worksheets en un mismo zip con un `sharedStrings` único.

**Tech Stack:** Kotlin Multiplatform / Compose Multiplatform, SQLDelight, Koin, material3 (DatePicker/DatePickerDialog/Surface); sin dependencias nuevas. `java.time` disponible en commonMain.

## Global Constraints

- El proyecto NO es un repositorio git (`D:\GestorGastos`) — **no hay commits**; verificar con gates de compilación/tests.
- Gates por tarea: `.\gradlew.bat :composeApp:jvmTest` (PASS) y `.\gradlew.bat :composeApp:compileKotlinJvm :composeApp:compileDebugKotlinAndroid` (BUILD SUCCESSFUL).
- No agregar dependencias nuevas.
- Seguir convenciones existentes: `AppTheme.*`, Material3, `Strings.t(...)`/`tr(...)`, `Strings.mes(indice)` es 1-based (Ene..Dic).
- Reusar claves i18n existentes (`registrar.seleccionarFecha`); NO se agregan claves nuevas.
- Nombres de hoja: `"${Strings.mes(mes)} $anio"` (ej. "Ene 2026"). Nombre de archivo sugerido: sin cambios.

---

### Task 1: Núcleo multi-hoja (modelo + use case + generador + ViewModel + tests)

Cambio atómico: no se puede compilar por partes (todos consumen `ExportarContenido`). Una sola tarea, bien acotada.

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/angel/gg/domain/model/Exportacion.kt` (reemplazar contenido completo)
- Modify: `composeApp/src/commonMain/kotlin/com/angel/gg/domain/usecase/ExportarGastosUseCase.kt` (reemplazar contenido completo)
- Modify: `composeApp/src/commonMain/kotlin/com/angel/gg/export/XlsxGenerador.kt` (reemplazar contenido completo)
- Modify: `composeApp/src/commonMain/kotlin/com/angel/gg/presentation/viewmodel/ExportarViewModel.kt` (reemplazar contenido completo)
- Modify: `composeApp/src/commonTest/kotlin/com/angel/gg/export/XlsxGeneradorTest.kt` (reemplazar contenido completo)

**Interfaces:**
- Consumes: `Gasto` (campos existentes; `gasto.fecha` es String ISO `YYYY-MM-DD`), `IngresoMensual(id, anio, mes, monto)`, `ExportarAlcance`, repositorios existentes, `ZipWriter.zip(listOf("nombre" to ByteArray))`, `Strings.mes`/`Strings.t`.
- Produces:
  - `data class HojaExcel(nombre: String, gastos: List<Gasto>, ingresoTotal: Double)`
  - `data class ExportarContenido(nombreArchivo: String, hojas: List<HojaExcel>)`
  - `data class ExportarDatos(gastos: List<Gasto>, ingresos: List<IngresoMensual>, desde: LocalDate, hasta: LocalDate, alcance: ExportarAlcance)`
  - `XlsxGenerador.generar(contenido: ExportarContenido): ByteArray` (book multi-sheet)
  - `ExportarViewModel.exportar(alcance, desde, hasta)` y `guardar()`/`limpiarMensaje()` sin cambios de firma.

- [ ] **Step 1: Modelo nuevo**

`Exportacion.kt` completo:

```kotlin
package com.angel.gg.domain.model

import java.time.LocalDate

enum class ExportarAlcance { MES_ACTUAL, ANIO_ACTUAL, TODO, PERSONALIZADO }

data class ExportarDatos(
    val gastos: List<Gasto>,
    val ingresos: List<IngresoMensual>,
    val desde: LocalDate,
    val hasta: LocalDate,
    val alcance: ExportarAlcance
)

data class HojaExcel(
    val nombre: String,
    val gastos: List<Gasto>,
    val ingresoTotal: Double
)

data class ExportarContenido(
    val nombreArchivo: String,
    val hojas: List<HojaExcel>
)
```

- [ ] **Step 2: Use case devuelve ingresos**

`ExportarGastosUseCase.kt` completo (único cambio funcional: devuelve `ingresos` en vez de la suma; queries intactas):

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
        val ingresos = ingresoMensualRepository.obtenerEnRango(desdeKey, hastaKey)

        return ExportarDatos(
            gastos = gastos,
            ingresos = ingresos,
            desde = fDesde,
            hasta = fHasta,
            alcance = alcance
        )
    }
}
```

- [ ] **Step 3: XlsxGenerador multi-hoja**

`XlsxGenerador.kt` completo (estructura refactorizada: `generar` iterar hojas, `hojaXml` privada por hoja, nuevo `workbookXml/contentTypesXml/workbookRelsXml` con N; `stylesXml`/`rootRelsXml`/`sharedStringsXml` sin cambios):

```kotlin
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
```

- [ ] **Step 4: ViewModel agrupa por mes**

`ExportarViewModel.kt` completo (se elimina `hojaDe`, se agregan `hojasDe`/`ingresoDe`; se conservan `exportar`/`guardar`/`limpiarMensaje`/`archivoDe` y el fix de feedback de fallo de `guardar()`):

```kotlin
package com.angel.gg.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.angel.gg.domain.model.ExportarAlcance
import com.angel.gg.domain.model.ExportarContenido
import com.angel.gg.domain.model.ExportarDatos
import com.angel.gg.domain.model.HojaExcel
import com.angel.gg.domain.model.IngresoMensual
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
            runCatching {
                val datos = exportarGastosUseCase.ejecutar(alcance, desde, hasta)
                if (datos.gastos.isEmpty()) {
                    _state.update {
                        it.copy(exportando = false, mensaje = Strings.t("exportar.sinDatos"), esError = true)
                    }
                } else {
                    val contenido = ExportarContenido(
                        nombreArchivo = archivoDe(alcance, datos),
                        hojas = hojasDe(datos)
                    )
                    val bytes = XlsxGenerador.generar(contenido)
                    _state.update {
                        it.copy(
                            exportando = false, bytes = bytes,
                            nombreArchivo = contenido.nombreArchivo, listoParaGuardar = true
                        )
                    }
                }
            }.onFailure {
                _state.update { it.copy(exportando = false, mensaje = Strings.t("exportar.error"), esError = true) }
            }
        }
    }

    private fun hojasDe(datos: ExportarDatos): List<HojaExcel> =
        datos.gastos
            .groupBy { gasto ->
                val fecha = LocalDate.parse(gasto.fecha)
                fecha.year to fecha.monthValue
            }
            .toSortedMap(compareBy { it.first * 100 + it.second })
            .map { (anioMes, gastosMes) ->
                HojaExcel(
                    nombre = "${Strings.mes(anioMes.second)} ${anioMes.first}",
                    gastos = gastosMes,
                    ingresoTotal = ingresoDe(datos.ingresos, anioMes.first, anioMes.second)
                )
            }

    private fun ingresoDe(ingresos: List<IngresoMensual>, anio: Int, mes: Int): Double =
        ingresos.firstOrNull { it.anio == anio && it.mes == mes }?.monto ?: 0.0

    fun guardar() {
        val actual = _state.value
        val bytes = actual.bytes ?: return
        val nombre = actual.nombreArchivo ?: return
        viewModelScope.launch {
            val ok = exportarGuardado.guardar(nombre, bytes)
            _state.update {
                it.copy(
                    listoParaGuardar = false, bytes = null, nombreArchivo = null,
                    mensaje = if (ok) Strings.t("exportar.exito") else Strings.t("exportar.error"),
                    esError = !ok
                )
            }
        }
    }

    fun limpiarMensaje() {
        _state.update { it.copy(mensaje = null, esError = false) }
    }

    private fun archivoDe(alcance: ExportarAlcance, datos: ExportarDatos): String =
        when (alcance) {
            ExportarAlcance.MES_ACTUAL -> "Gastos ${Strings.mes(datos.desde.monthValue)} ${datos.desde.year}.xlsx"
            ExportarAlcance.ANIO_ACTUAL -> "Gastos ${datos.desde.year}.xlsx"
            ExportarAlcance.TODO -> "Gastos.xlsx"
            ExportarAlcance.PERSONALIZADO -> "Gastos ${datos.desde} a ${datos.hasta}.xlsx"
        }
}
```

- [ ] **Step 5: Tests actualizados + nuevos**

`XlsxGeneradorTest.kt` completo (los 2 tests de generación se adaptan a la nueva API; se agregan 2 tests multi-hoja):

```kotlin
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
```

Nota: `"Luz".toRegex()` aparece una única vez porque los strings compartidos solo se escriben en
`sharedStrings.xml` y ambas hojas referencian el mismo índice. Si el assert falla, verificar que el
texto `Luz` no se repita en el zip (nombres de archivo, etc.).

- [ ] **Step 6: Gates**

Run: `.\gradlew.bat :composeApp:jvmTest`
Run: `.\gradlew.bat :composeApp:compileKotlinJvm :composeApp:compileDebugKotlinAndroid`
Expected: tests PASS (7/7) y BUILD SUCCESSFUL.

---

### Task 2: Fix DatePicker del rango Personalizado (FechaCampo)

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/angel/gg/presentation/screen/ExportarDialog.kt`

**Interfaces:**
- Consumes: `AppTheme.CardDark/BorderColor/TextMuted/TextSubtle/TextPrimary`, `AppIcons.Calendario` (mismo paquete), `tr("registrar.seleccionarFecha")` (clave existente), `Surface(onClick=…)` M3.
- Produces: `FechaCampo(etiqueta, valor, modifier, onClick)` con la misma firma (los call sites del diálogo no cambian).

- [ ] **Step 1: Imports**

En `ExportarDialog.kt`:
- Quitar: `import androidx.compose.foundation.clickable` (deja de usarse)
- Agregar: `import androidx.compose.foundation.BorderStroke`
- Agregar: `import androidx.compose.foundation.shape.RoundedCornerShape`

- [ ] **Step 2: Reemplazar FechaCampo**

Reemplazar el body de `FechaCampo` (líneas 155-180 actuales) por el patrón `Surface(onClick=…)`
que usan Registrar/Editar (sin `OutlinedTextField`):

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FechaCampo(
    etiqueta: String,
    valor: LocalDate?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(modifier = modifier) {
        Text(etiqueta, color = AppTheme.TextMuted, fontSize = 12.sp)
        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(4.dp),
            color = AppTheme.CardDark,
            border = BorderStroke(1.dp, AppTheme.BorderColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    valor?.toString() ?: tr("registrar.seleccionarFecha"),
                    color = if (valor == null) AppTheme.TextSubtle else AppTheme.TextPrimary,
                    fontSize = 14.sp
                )
                Icon(
                    AppIcons.Calendario,
                    contentDescription = tr("registrar.seleccionarFecha"),
                    tint = AppTheme.TextMuted
                )
            }
        }
    }
}
```

- [ ] **Step 3: Gates**

Run: `.\gradlew.bat :composeApp:jvmTest`
Run: `.\gradlew.bat :composeApp:compileKotlinJvm :composeApp:compileDebugKotlinAndroid`
Expected: tests PASS y BUILD SUCCESSFUL (sin warnings de imports sin usar).

- [ ] **Step 4: Verificación manual (desktop)**

Run: `.\gradlew.bat :composeApp:run`
Ajustes → Exportar → alcance "Personalizado" → clic en Desde/Hasta → debe abrirse el `DatePickerDialog`;
luego en Mes actual / Año actual / Todo: el archivo generado debe abrirse en Excel/WPS con **una hoja por mes**
("Ene 2026", "Feb 2026", …), solo meses con gastos; cada hoja con su saldo/balance/porcentaje por mes.