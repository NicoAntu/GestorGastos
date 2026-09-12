package com.angel.gg.domain.usecase

import com.angel.gg.domain.model.Gasto
import com.angel.gg.domain.repository.GastoRepository
import com.angel.gg.domain.repository.CategoriaRepository


class RegistrarGastoUseCase(
    private val gastoRepository: GastoRepository,
    private val categoriaRepository: CategoriaRepository
) {
    sealed class Error {
        data object MontoInvalido       : Error()
        data object DescripcionVacia    : Error()
        data object CategoriaInexistente: Error()
    }

    sealed class Resultado {
        data class Exito(val gasto: Gasto) : Resultado()
        data class Fallo(val error: Error) : Resultado()
    }

    suspend operator fun invoke(
        monto       : Double,
        descripcion : String,
        categoriaId : String,
        // Fecha opcional — si es null el sistema asigna la fecha actual (Tarjeta 1, CA3)
        fecha       : String? = null
    ): Resultado {

        // Validación de monto
        if (monto <= 0.0) return Resultado.Fallo(Error.MontoInvalido)

        // Validación de descripción
        if (descripcion.isBlank()) return Resultado.Fallo(Error.DescripcionVacia)

        // Validación de categoría existente
        val categoria = categoriaRepository.obtenerPorId(categoriaId)
            ?: return Resultado.Fallo(Error.CategoriaInexistente)

        // Fecha del sistema si no se especifica (Tarjeta 1, CA3)
        val fechaFinal = fecha ?: obtenerFechaHoy()

        // Extraer año y mes de la fecha para la navegación mensual (Tarjeta 6)
        val (anio, mes) = parsearFecha(fechaFinal)

        val gasto = Gasto(
            id              = generarId(),
            monto           = monto,
            descripcion     = descripcion.trim(),
            categoriaId     = categoriaId,
            categoriaNombre = categoria.nombre,
            categoriaColor  = categoria.color,
            categoriaIcono  = categoria.icono,
            fecha           = fechaFinal,
            anio            = anio,
            mes             = mes
            // cuotaActual, cuotaTotal e idPadre quedan null — gasto simple
        )

        gastoRepository.insertar(gasto)
        return Resultado.Exito(gasto)
    }
}