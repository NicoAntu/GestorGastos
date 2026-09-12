package com.angel.gg.data.repository

import com.angel.gg.data.db.toFlow
import com.angel.gg.db.GastosDatabase
import com.angel.gg.domain.model.Categoria
import com.angel.gg.domain.repository.CategoriaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class CategoriaRepositoryImpl(
    private val database: GastosDatabase
) : CategoriaRepository {

    private val queries = database.categoriaQueries

    override fun obtenerTodas(): Flow<List<Categoria>> =
        queries.obtenerTodas()
            .toFlow()
            .map { list -> list.map { it.toDomain() } }

    override suspend fun obtenerPorId(id: String): Categoria? =
        withContext(Dispatchers.Default) {
            queries.obtenerPorId(id).executeAsOneOrNull()?.toDomain()
        }

    override suspend fun insertar(categoria: Categoria) =
        withContext(Dispatchers.Default) {
            queries.insertar(
                id          = categoria.id,
                nombre      = categoria.nombre,
                descripcion = categoria.descripcion,
                color       = categoria.color,
                icono       = categoria.icono,
                orden       = categoria.orden.toLong(),
                fijada      = if (categoria.fijada) 1L else 0L
            )
        }

    override suspend fun actualizar(categoria: Categoria) =
        withContext(Dispatchers.Default) {
            queries.actualizar(
                nombre      = categoria.nombre,
                descripcion = categoria.descripcion,
                color       = categoria.color,
                icono       = categoria.icono,
                orden       = categoria.orden.toLong(),
                fijada      = if (categoria.fijada) 1L else 0L,
                id          = categoria.id
            )
        }

    override suspend fun actualizarOrden(id: String, orden: Int) =
        withContext(Dispatchers.Default) {
            queries.actualizarOrden(orden = orden.toLong(), id = id)
        }

    override suspend fun eliminar(id: String) =
        withContext(Dispatchers.Default) {
            queries.eliminar(id)
        }

    private fun com.angel.gg.db.Categoria.toDomain() = Categoria(
        id          = id,
        nombre      = nombre,
        descripcion = descripcion,
        color       = color,
        icono       = icono,
        orden       = orden.toInt(),
        fijada      = fijada == 1L
    )
}