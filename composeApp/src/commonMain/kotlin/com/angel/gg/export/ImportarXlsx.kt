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
                if (monto != null && monto > 0.0) {
                    val descIdx = valorCelda(bloque, "B$r")?.toIntOrNull()
                    val catIdx = valorCelda(bloque, "C$r")?.toIntOrNull()
                    val fecha = valorCelda(bloque, "D$r")?.let { v ->
                        if (v.isBlank()) null
                        else v.toDoubleOrNull()?.let { BASE_EXCEL.plusDays(it.toLong()) }
                            ?: runCatching { LocalDate.parse(v) }.getOrNull()
                    } ?: LocalDate.now().withDayOfMonth(1)
                    filas.add(
                        FilaImportada(
                            monto = monto,
                            descripcion = descIdx?.let { shared.getOrNull(it) }?.takeIf { it.isNotBlank() } ?: "-",
                            categoriaNombre = catIdx?.let { shared.getOrNull(it) }?.takeIf { it.isNotBlank() } ?: "-",
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