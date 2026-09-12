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