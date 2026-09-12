package com.angel.gg.domain.usecase

import com.angel.gg.domain.model.Categoria
import com.angel.gg.domain.repository.CategoriaRepository
import com.angel.gg.domain.repository.GastoRepository
import kotlinx.coroutines.launch

class GestionarCategoriaUseCase(
    private val categoriaRepository: CategoriaRepository,
    private val gastoRepository    : GastoRepository
) {
    sealed class Error {
        data object NombreVacio          : Error()
        data object ColorInvalido        : Error()
        data object CategoriaInexistente : Error()
        data class CategoriaConGastos(val cantidad: Int) : Error()
    }

    sealed class Resultado {
        data class Exito(val categoria: Categoria) : Resultado()
        data object ExitoSinRetorno                : Resultado()
        data class Fallo(val error: Error)         : Resultado()
    }

    // ── Crear ────────────────────────────────────────────────────────────
    suspend fun crear(
        nombre     : String,
        descripcion: String? = null,
        color      : String  = "#6200EE",
        icono      : String  = "categoria",
        fijada     : Boolean = false
    ): Resultado {

        if (nombre.isBlank())
            return Resultado.Fallo(Error.NombreVacio)

        if (!color.matches(Regex("^#([A-Fa-f0-9]{6})$")))
            return Resultado.Fallo(Error.ColorInvalido)

        // El orden se asigna al final de la lista actual
        val todasLasCategorias = categoriaRepository
            .obtenerTodas()
            .let { flow ->
                // Leer el valor actual del flow sin suscribirse
                var lista = emptyList<Categoria>()
                val job = kotlinx.coroutines.CoroutineScope(
                    kotlinx.coroutines.Dispatchers.Default
                ).launch {
                    flow.collect {
                        lista = it
                        return@collect
                    }
                }
                job.cancel()
                lista
            }

        val siguienteOrden = todasLasCategorias.size

        val categoria = Categoria(
            id          = generarId(),
            nombre      = nombre.trim(),
            descripcion = descripcion?.trim(),
            color       = color.uppercase(),
            icono       = icono,
            orden       = siguienteOrden,
            fijada      = fijada
        )

        categoriaRepository.insertar(categoria)
        return Resultado.Exito(categoria)
    }

    // ── Editar ───────────────────────────────────────────────────────────
    suspend fun editar(
        id         : String,
        nombre     : String,
        descripcion: String? = null,
        color      : String,
        icono      : String,
        fijada     : Boolean
    ): Resultado {

        if (nombre.isBlank())
            return Resultado.Fallo(Error.NombreVacio)

        if (!color.matches(Regex("^#([A-Fa-f0-9]{6})$")))
            return Resultado.Fallo(Error.ColorInvalido)

        val existente = categoriaRepository.obtenerPorId(id)
            ?: return Resultado.Fallo(Error.CategoriaInexistente)

        categoriaRepository.actualizar(
            existente.copy(
                nombre      = nombre.trim(),
                descripcion = descripcion?.trim(),
                color       = color.uppercase(),
                icono       = icono,
                fijada      = fijada
            )
        )
        return Resultado.ExitoSinRetorno
    }

    // ── Reordenar ────────────────────────────────────────────────────────
    // Recibe la lista completa ya reordenada por el usuario (drag & drop)
    // y actualiza el campo orden de cada categoría (Tarjeta 2, CA3)
    suspend fun reordenar(categorias: List<Categoria>): Resultado {
        categorias.forEachIndexed { indice, categoria ->
            categoriaRepository.actualizarOrden(
                id    = categoria.id,
                orden = indice
            )
        }
        return Resultado.ExitoSinRetorno
    }

    // ── Eliminar ─────────────────────────────────────────────────────────
    suspend fun eliminar(id: String): Resultado {
        categoriaRepository.obtenerPorId(id)
            ?: return Resultado.Fallo(Error.CategoriaInexistente)

        val cantidadGastos = gastoRepository.contarGastosPorCategoria(id)
        if (cantidadGastos > 0)
            return Resultado.Fallo(Error.CategoriaConGastos(cantidadGastos))

        categoriaRepository.eliminar(id)
        return Resultado.ExitoSinRetorno
    }
}