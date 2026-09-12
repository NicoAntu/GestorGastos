package com.angel.gg.data.repository

import com.angel.gg.data.db.toFlowOne
import com.angel.gg.db.GastosDatabase
import com.angel.gg.domain.model.IngresoMensual
import com.angel.gg.domain.repository.IngresoMensualRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class IngresoMensualRepositoryImpl(
    private val database: GastosDatabase
) : IngresoMensualRepository {

    private val queries = database.ingresoMensualQueries

    override fun obtenerPorMes(anio: Int, mes: Int): Flow<IngresoMensual?> =
        queries.obtenerPorMes(anio = anio.toLong(), mes = mes.toLong())
            .toFlowOne()
            .map { it?.toDomain() }

    override suspend fun guardar(ingresoMensual: IngresoMensual) =
        withContext(Dispatchers.Default) {
            queries.insertar(
                id    = ingresoMensual.id,
                anio  = ingresoMensual.anio.toLong(),
                mes   = ingresoMensual.mes.toLong(),
                monto = ingresoMensual.monto
            )
        }

    override suspend fun obtenerEnRango(desdeKey: Long, hastaKey: Long): List<IngresoMensual> =
        withContext(Dispatchers.Default) {
            queries.obtenerEnRango(value_ = desdeKey, value__ = hastaKey)
                .executeAsList()
                .map { it.toDomain() }
        }

    private fun com.angel.gg.db.IngresoMensual.toDomain() = IngresoMensual(
        id    = id,
        anio  = anio.toInt(),
        mes   = mes.toInt(),
        monto = monto
    )
}