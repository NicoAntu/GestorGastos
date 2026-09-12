package com.angel.gg.domain.usecase

import com.angel.gg.domain.model.Categoria
import com.angel.gg.domain.model.FilaImportada
import com.angel.gg.domain.model.Gasto
import com.angel.gg.domain.model.IngresoMensual
import com.angel.gg.domain.model.ImportacionResultado
import com.angel.gg.domain.model.ResultadoImportacion
import com.angel.gg.domain.repository.CategoriaRepository
import com.angel.gg.domain.repository.GastoCategoriaDb
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