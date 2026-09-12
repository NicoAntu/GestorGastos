package com.angel.gg.domain.repository

import com.angel.gg.domain.model.IngresoMensual
import kotlinx.coroutines.flow.Flow

interface IngresoMensualRepository {
    fun obtenerPorMes(anio: Int, mes: Int): Flow<IngresoMensual?>
    suspend fun guardar(ingresoMensual: IngresoMensual)
    suspend fun obtenerEnRango(desdeKey: Long, hastaKey: Long): List<IngresoMensual>
}