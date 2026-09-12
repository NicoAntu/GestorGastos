package com.angel.gg.domain.repository

import com.angel.gg.domain.model.Gasto
import kotlinx.coroutines.flow.Flow

data class GastoCategoriaDb(
    val categoriaId    : String,
    val categoriaNombre: String,
    val categoriaColor : String,
    val total          : Double
)
interface GastoRepository {
    fun obtenerPorMes(anio: Int, mes: Int): Flow<List<Gasto>>
    fun obtenerGastosPorCategoriaMes(anio: Int, mes: Int): Flow<List<GastoCategoriaDb>>
    suspend fun obtenerPorId(id: String): Gasto?
    suspend fun obtenerPorIdPadre(idPadre: String): List<Gasto>
    suspend fun buscarPorDescripcion(query: String): List<Gasto>
    suspend fun totalPorMes(anio: Int, mes: Int): Double
    suspend fun obtenerPorRango(desde: String, hasta: String): List<Gasto>

    // Inserción simple (un gasto o una cuota ya calculada)
    suspend fun insertar(gasto: Gasto)

    // Inserción de toda la serie de cuotas de una vez
    suspend fun insertarSerieCuotas(gastos: List<Gasto>)

    // Edición con alcance: solo este, cuotas futuras, o toda la serie
    suspend fun actualizarUno(gasto: Gasto)
    suspend fun actualizarCuotasFuturas(
        idPadre: String,
        desdeCuota: Int,
        monto: Double,
        descripcion: String,
        categoriaId: String
    )
    suspend fun actualizarTodaSerie(
        idPadre: String,
        monto: Double,
        descripcion: String,
        categoriaId: String
    )

    // Eliminación con alcance
    suspend fun eliminarUno(id: String)
    suspend fun eliminarCuotasFuturas(idPadre: String, desdeCuota: Int)
    suspend fun eliminarTodaSerie(idPadre: String)

    // Cantidad de gastos registrados en una categoría
    suspend fun contarGastosPorCategoria(categoriaId: String): Int
}