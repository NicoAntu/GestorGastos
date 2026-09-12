package com.angel.gg.data.repository

import com.angel.gg.data.db.toFlow
import com.angel.gg.db.GastosDatabase
import com.angel.gg.domain.model.Gasto
import com.angel.gg.domain.repository.GastoCategoriaDb
import com.angel.gg.domain.repository.GastoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class GastoRepositoryImpl(
    private val database: GastosDatabase
) : GastoRepository {

    private val queries = database.gastoQueries

    // ── Queries reactivas (Flow) ─────────────────────────────────────────

    override fun obtenerPorMes(anio: Int, mes: Int): Flow<List<Gasto>> =
        queries.obtenerPorMes(anio = anio.toLong(), mes = mes.toLong())
            .toFlow()
            .map { list -> list.map { it.toDomain() } }

    override fun obtenerGastosPorCategoriaMes(
        anio: Int,
        mes : Int
    ): Flow<List<GastoCategoriaDb>> =
        queries.totalPorCategoriaMes(
            anio = anio.toLong(),
            mes  = mes.toLong()
        )
            .toFlow()
            .map { list ->
                list.map { row ->
                    GastoCategoriaDb(
                        categoriaId     = row.id,
                        categoriaNombre = row.nombre,
                        categoriaColor  = row.color,
                        total           = row.total ?: 0.0
                    )
                }
            }

    // ── Queries simples (suspend) ────────────────────────────────────────

    override suspend fun obtenerPorId(id: String): Gasto? =
        withContext(Dispatchers.Default) {
            queries.obtenerPorId(id).executeAsOneOrNull()?.toDomain()
        }

    override suspend fun obtenerPorIdPadre(idPadre: String): List<Gasto> =
        withContext(Dispatchers.Default) {
            queries.obtenerPorIdPadre(idPadre)
                .executeAsList()
                .map { it.toSimpleDomain() }
        }

    override suspend fun buscarPorDescripcion(query: String): List<Gasto> =
        withContext(Dispatchers.Default) {
            queries.buscarPorDescripcion(query)
                .executeAsList()
                .map { it.toDomain() }
        }

    override suspend fun totalPorMes(anio: Int, mes: Int): Double =
        withContext(Dispatchers.Default) {
            queries.totalPorMes(
                anio = anio.toLong(),
                mes  = mes.toLong()
            ).executeAsOne()
        }

    override suspend fun obtenerPorRango(desde: String, hasta: String): List<Gasto> =
        withContext(Dispatchers.Default) {
            queries.obtenerPorRango(fecha = desde, fecha_ = hasta)
                .executeAsList()
                .map { it.toDomain() }
        }

    // ── Escritura ────────────────────────────────────────────────────────

    override suspend fun insertar(gasto: Gasto) =
        withContext(Dispatchers.Default) {
            queries.insertar(
                id           = gasto.id,
                monto        = gasto.monto,
                descripcion  = gasto.descripcion,
                categoria_id = gasto.categoriaId,
                fecha        = gasto.fecha,
                anio         = gasto.anio.toLong(),
                mes          = gasto.mes.toLong(),
                cuota_actual = gasto.cuotaActual?.toLong(),
                cuota_total  = gasto.cuotaTotal?.toLong(),
                id_padre     = gasto.idPadre
            )
        }

    override suspend fun insertarSerieCuotas(gastos: List<Gasto>) =
        withContext(Dispatchers.Default) {
            database.transaction {
                gastos.forEach { gasto ->
                    queries.insertar(
                        id           = gasto.id,
                        monto        = gasto.monto,
                        descripcion  = gasto.descripcion,
                        categoria_id = gasto.categoriaId,
                        fecha        = gasto.fecha,
                        anio         = gasto.anio.toLong(),
                        mes          = gasto.mes.toLong(),
                        cuota_actual = gasto.cuotaActual?.toLong(),
                        cuota_total  = gasto.cuotaTotal?.toLong(),
                        id_padre     = gasto.idPadre
                    )
                }
            }
        }

    override suspend fun actualizarUno(gasto: Gasto) =
        withContext(Dispatchers.Default) {
            queries.actualizar(
                monto        = gasto.monto,
                descripcion  = gasto.descripcion,
                categoria_id = gasto.categoriaId,
                fecha        = gasto.fecha,
                anio         = gasto.anio.toLong(),
                mes          = gasto.mes.toLong(),
                id           = gasto.id
            )
        }

    override suspend fun actualizarCuotasFuturas(
        idPadre    : String,
        desdeCuota : Int,
        monto      : Double,
        descripcion: String,
        categoriaId: String
    ) = withContext(Dispatchers.Default) {
        queries.actualizarCuotasFuturas(
            monto        = monto,
            descripcion  = descripcion,
            categoria_id = categoriaId,
            id_padre     = idPadre,
            cuota_actual = desdeCuota.toLong()
        )
    }

    override suspend fun actualizarTodaSerie(
        idPadre    : String,
        monto      : Double,
        descripcion: String,
        categoriaId: String
    ) = withContext(Dispatchers.Default) {
        queries.actualizarTodaSerie(
            monto        = monto,
            descripcion  = descripcion,
            categoria_id = categoriaId,
            id_padre     = idPadre
        )
    }

    override suspend fun eliminarUno(id: String) =
        withContext(Dispatchers.Default) {
            queries.eliminar(id)
        }

    override suspend fun eliminarCuotasFuturas(idPadre: String, desdeCuota: Int) =
        withContext(Dispatchers.Default) {
            queries.eliminarCuotasFuturas(
                id_padre     = idPadre,
                cuota_actual = desdeCuota.toLong()
            )
        }

    override suspend fun eliminarTodaSerie(idPadre: String) =
        withContext(Dispatchers.Default) {
            queries.eliminarTodaSerie(idPadre)
        }

    override suspend fun contarGastosPorCategoria(categoriaId: String): Int =
        withContext(Dispatchers.Default) {
            queries.contarPorCategoria(categoriaId)
                .executeAsOne()
                .toInt()
        }

    // ── Mapeos ───────────────────────────────────────────────────────────

    // Queries con JOIN — SQLDelight genera una data class por cada query
    private fun com.angel.gg.db.ObtenerPorMes.toDomain() = Gasto(
        id              = id,
        monto           = monto,
        descripcion     = descripcion,
        categoriaId     = categoria_id,
        categoriaNombre = categoria_nombre,
        categoriaColor  = categoria_color,
        categoriaIcono  = categoria_icono,
        fecha           = fecha,
        anio            = anio.toInt(),
        mes             = mes.toInt(),
        cuotaActual     = cuota_actual?.toInt(),
        cuotaTotal      = cuota_total?.toInt(),
        idPadre         = id_padre
    )

    private fun com.angel.gg.db.ObtenerPorId.toDomain() = Gasto(
        id              = id,
        monto           = monto,
        descripcion     = descripcion,
        categoriaId     = categoria_id,
        categoriaNombre = categoria_nombre,
        categoriaColor  = categoria_color,
        categoriaIcono  = categoria_icono,
        fecha           = fecha,
        anio            = anio.toInt(),
        mes             = mes.toInt(),
        cuotaActual     = cuota_actual?.toInt(),
        cuotaTotal      = cuota_total?.toInt(),
        idPadre         = id_padre
    )

    private fun com.angel.gg.db.BuscarPorDescripcion.toDomain() = Gasto(
        id              = id,
        monto           = monto,
        descripcion     = descripcion,
        categoriaId     = categoria_id,
        categoriaNombre = categoria_nombre,
        categoriaColor  = categoria_color,
        categoriaIcono  = categoria_icono,
        fecha           = fecha,
        anio            = anio.toInt(),
        mes             = mes.toInt(),
        cuotaActual     = cuota_actual?.toInt(),
        cuotaTotal      = cuota_total?.toInt(),
        idPadre         = id_padre
    )

    private fun com.angel.gg.db.ObtenerPorRango.toDomain() = Gasto(
        id              = id,
        monto           = monto,
        descripcion     = descripcion,
        categoriaId     = categoria_id,
        categoriaNombre = categoria_nombre,
        categoriaColor  = categoria_color,
        categoriaIcono  = categoria_icono,
        fecha           = fecha,
        anio            = anio.toInt(),
        mes             = mes.toInt(),
        cuotaActual     = cuota_actual?.toInt(),
        cuotaTotal      = cuota_total?.toInt(),
        idPadre         = id_padre
    )

    // Queries simples sin JOIN — devuelven la tabla directa
    private fun com.angel.gg.db.Gasto.toSimpleDomain() = Gasto(
        id              = id,
        monto           = monto,
        descripcion     = descripcion,
        categoriaId     = categoria_id,
        categoriaNombre = "",
        categoriaColor  = "",
        categoriaIcono  = "",
        fecha           = fecha,
        anio            = anio.toInt(),
        mes             = mes.toInt(),
        cuotaActual     = cuota_actual?.toInt(),
        cuotaTotal      = cuota_total?.toInt(),
        idPadre         = id_padre
    )
}