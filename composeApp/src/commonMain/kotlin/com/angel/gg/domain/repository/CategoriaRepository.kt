package com.angel.gg.domain.repository

import com.angel.gg.domain.model.Categoria
import kotlinx.coroutines.flow.Flow

interface CategoriaRepository {
    // Flow para que la UI se actualice automáticamente ante cambios
    fun obtenerTodas(): Flow<List<Categoria>>
    suspend fun obtenerPorId(id: String): Categoria?
    suspend fun insertar(categoria: Categoria)
    suspend fun actualizar(categoria: Categoria)
    suspend fun actualizarOrden(id: String, orden: Int)
    suspend fun eliminar(id: String)
}