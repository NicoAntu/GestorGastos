package com.angel.gg.domain.usecase

import com.angel.gg.domain.model.IngresoMensual
import com.angel.gg.domain.repository.IngresoMensualRepository

class GuardarIngresoMensualUseCase(
    private val ingresoMensualRepository: IngresoMensualRepository
) {
    sealed class Error {
        data object MontoInvalido : Error()
    }

    sealed class Resultado {
        data class Exito(val ingreso: IngresoMensual) : Resultado()
        data class Fallo(val error: Error)            : Resultado()
    }

    suspend operator fun invoke(
        anio : Int,
        mes  : Int,
        monto: Double
    ): Resultado {

        if (monto < 0.0)
            return Resultado.Fallo(Error.MontoInvalido)

        val ingreso = IngresoMensual(
            id    = generarId(),
            anio  = anio,
            mes   = mes,
            monto = monto
        )

        // INSERT OR REPLACE — si ya existe un ingreso para ese mes lo sobreescribe
        // Esto implementa el CA del apartado específico por mes (Tarjeta 3)
        ingresoMensualRepository.guardar(ingreso)
        return Resultado.Exito(ingreso)
    }
}