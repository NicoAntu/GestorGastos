package com.angel.gg.domain.usecase

import com.angel.gg.domain.model.Gasto
import com.angel.gg.domain.repository.CategoriaRepository
import com.angel.gg.domain.repository.GastoRepository

class EditarCuotasUseCase(
    private val gastoRepository    : GastoRepository,
    private val categoriaRepository: CategoriaRepository
) {
    // Los tres alcances posibles de edición (Tarjeta 5, CA2)
    enum class AlcanceEdicion {
        SOLO_ESTA,       // Solo modifica la cuota seleccionada
        FUTURAS,         // Modifica esta cuota y todas las siguientes de la serie
        TODA_LA_SERIE    // Modifica todas las cuotas sin importar cuál está seleccionada
    }

    sealed class Error {
        data object MontoInvalido        : Error()
        data object DescripcionVacia     : Error()
        data object CategoriaInexistente : Error()
        data object GastoNoEncontrado    : Error()
        data object NoEsUnaCuota         : Error()
    }

    sealed class Resultado {
        data object Exito                : Resultado()
        data class Fallo(val error: Error) : Resultado()
    }

    suspend operator fun invoke(
        gastoId    : String,
        monto      : Double,
        descripcion: String,
        categoriaId: String,
        alcance    : AlcanceEdicion
    ): Resultado {

        // ── Validaciones ────────────────────────────────────────────────
        if (monto <= 0.0)
            return Resultado.Fallo(Error.MontoInvalido)

        if (descripcion.isBlank())
            return Resultado.Fallo(Error.DescripcionVacia)

        categoriaRepository.obtenerPorId(categoriaId)
            ?: return Resultado.Fallo(Error.CategoriaInexistente)

        val gastoActual = gastoRepository.obtenerPorId(gastoId)
            ?: return Resultado.Fallo(Error.GastoNoEncontrado)

        // Solo se puede editar con alcance si el gasto pertenece a una serie
        if (gastoActual.idPadre == null)
            return Resultado.Fallo(Error.NoEsUnaCuota)

        // ── Aplicar edición según alcance ────────────────────────────────
        when (alcance) {

            AlcanceEdicion.SOLO_ESTA -> {
                // Modifica únicamente el registro seleccionado
                gastoRepository.actualizarUno(
                    gastoActual.copy(
                        monto       = monto,
                        descripcion = descripcion.trim(),
                        categoriaId = categoriaId
                    )
                )
            }

            AlcanceEdicion.FUTURAS -> {
                // Modifica esta cuota y todas las que vienen después en la serie
                // Usa cuotaActual como punto de corte (Tarjeta 5, CA2)
                gastoRepository.actualizarCuotasFuturas(
                    idPadre    = gastoActual.idPadre,
                    desdeCuota = gastoActual.cuotaActual!!,
                    monto      = monto,
                    descripcion = descripcion.trim(),
                    categoriaId = categoriaId
                )
            }

            AlcanceEdicion.TODA_LA_SERIE -> {
                // Modifica todos los registros de la serie sin excepción
                gastoRepository.actualizarTodaSerie(
                    idPadre     = gastoActual.idPadre,
                    monto       = monto,
                    descripcion = descripcion.trim(),
                    categoriaId = categoriaId
                )
            }
        }

        return Resultado.Exito
    }
}