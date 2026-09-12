package com.angel.gg.domain.usecase

import com.angel.gg.domain.repository.GastoRepository

class EliminarCuotasUseCase(
    private val gastoRepository: GastoRepository
) {
    enum class AlcanceEliminacion {
        SOLO_ESTA,
        FUTURAS,
        TODA_LA_SERIE
    }

    sealed class Error {
        data object GastoNoEncontrado : Error()
        data object NoEsUnaCuota      : Error()
    }

    sealed class Resultado {
        data object Exito                  : Resultado()
        data class Fallo(val error: Error) : Resultado()
    }

    suspend operator fun invoke(
        gastoId: String,
        alcance: AlcanceEliminacion
    ): Resultado {

        val gastoActual = gastoRepository.obtenerPorId(gastoId)
            ?: return Resultado.Fallo(Error.GastoNoEncontrado)

        // Si no es una cuota, usar directamente eliminarUno sin preguntar alcance
        if (gastoActual.idPadre == null)
            return Resultado.Fallo(Error.NoEsUnaCuota)

        when (alcance) {

            AlcanceEliminacion.SOLO_ESTA -> {
                gastoRepository.eliminarUno(gastoId)
            }

            AlcanceEliminacion.FUTURAS -> {
                gastoRepository.eliminarCuotasFuturas(
                    idPadre    = gastoActual.idPadre,
                    desdeCuota = gastoActual.cuotaActual!!
                )
            }

            AlcanceEliminacion.TODA_LA_SERIE -> {
                gastoRepository.eliminarTodaSerie(gastoActual.idPadre)
            }
        }

        return Resultado.Exito
    }
}