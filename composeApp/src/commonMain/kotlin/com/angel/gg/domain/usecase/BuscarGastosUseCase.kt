package com.angel.gg.domain.usecase

import com.angel.gg.domain.model.Gasto
import com.angel.gg.domain.repository.GastoRepository

class BuscarGastosUseCase(
    private val gastoRepository: GastoRepository
) {
    sealed class Error {
        data object QueryVacia          : Error()
        data object QueryDemasiadoCorta : Error()
    }

    sealed class Resultado {
        data class Exito(val resultados: List<Gasto>) : Resultado()
        data class SinResultados(val query: String)   : Resultado()
        data class Fallo(val error: Error)            : Resultado()
    }

    suspend operator fun invoke(query: String): Resultado {

        // Ignorar búsquedas vacías
        if (query.isBlank())
            return Resultado.Fallo(Error.QueryVacia)

        // Mínimo 2 caracteres para evitar resultados masivos
        if (query.trim().length < 2)
            return Resultado.Fallo(Error.QueryDemasiadoCorta)

        val resultados = gastoRepository.buscarPorDescripcion(query.trim())

        return if (resultados.isEmpty())
            Resultado.SinResultados(query.trim())
        else
            Resultado.Exito(resultados)
    }
}