package com.angel.gg.domain.usecase

import com.angel.gg.domain.model.Gasto
import com.angel.gg.domain.repository.GastoRepository

class ObtenerCronogramaCuotasUseCase(
    private val gastoRepository: GastoRepository
) {
    sealed class Error {
        data object GastoNoEncontrado : Error()
        data object NoEsUnaCuota      : Error()
    }

    sealed class Resultado {
        // Devuelve la lista completa ordenada por número de cuota
        data class Exito(
            val cronograma  : List<Gasto>,
            val cuotaActual : Gasto
        ) : Resultado()
        data class Fallo(val error: Error) : Resultado()
    }

    suspend operator fun invoke(gastoId: String): Resultado {

        val gasto = gastoRepository.obtenerPorId(gastoId)
            ?: return Resultado.Fallo(Error.GastoNoEncontrado)

        if (gasto.idPadre == null)
            return Resultado.Fallo(Error.NoEsUnaCuota)

        // Obtiene todas las cuotas de la serie ordenadas por cuotaActual ASC
        val cronograma = gastoRepository.obtenerPorIdPadre(gasto.idPadre)

        return Resultado.Exito(
            cronograma  = cronograma,
            cuotaActual = gasto
        )
    }
}