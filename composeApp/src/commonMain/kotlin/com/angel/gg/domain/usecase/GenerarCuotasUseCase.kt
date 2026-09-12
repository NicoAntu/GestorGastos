package com.angel.gg.domain.usecase

import com.angel.gg.domain.model.Gasto
import com.angel.gg.domain.repository.CategoriaRepository
import com.angel.gg.domain.repository.GastoRepository

class GenerarCuotasUseCase(
    private val gastoRepository    : GastoRepository,
    private val categoriaRepository: CategoriaRepository
) {
    sealed class Error {
        data object MontoInvalido        : Error()
        data object DescripcionVacia     : Error()
        data object CategoriaInexistente : Error()
        data object CantidadCuotasInvalida : Error()
    }

    sealed class Resultado {
        // Devuelve toda la serie generada para que el ViewModel pueda mostrarla
        data class Exito(val serie: List<Gasto>) : Resultado()
        data class Fallo(val error: Error)       : Resultado()
    }

    suspend operator fun invoke(
        monto      : Double,
        descripcion: String,
        categoriaId: String,
        // Fecha de la primera cuota — si es null usa la fecha del sistema
        fechaInicio: String? = null,
        cantCuotas : Int
    ): Resultado {

        // ── Validaciones ────────────────────────────────────────────────
        if (monto <= 0.0)
            return Resultado.Fallo(Error.MontoInvalido)

        if (descripcion.isBlank())
            return Resultado.Fallo(Error.DescripcionVacia)

        // Mínimo 2 cuotas — si es 1 usar RegistrarGastoUseCase directamente
        if (cantCuotas < 2)
            return Resultado.Fallo(Error.CantidadCuotasInvalida)

        val categoria = categoriaRepository.obtenerPorId(categoriaId)
            ?: return Resultado.Fallo(Error.CategoriaInexistente)

        // ── Generación de la serie ───────────────────────────────────────
        val fechaPrimeraCuota = fechaInicio ?: obtenerFechaHoy()

        // ID padre compartido por toda la serie (Tarjeta 4, CA4)
        val idPadre = generarId()

        // Monto de cada cuota redondeado a 2 decimales
        val montoCuota = redondear(monto / cantCuotas)

        // Diferencia de centavos por redondeo — se suma a la última cuota
        // para que la suma de todas las cuotas sea exactamente igual al total
        val montoUltimaCuota = redondear(monto - (montoCuota * (cantCuotas - 1)))

        val serie = (1..cantCuotas).map { numeroCuota ->
            // Proyectar la fecha sumando (numeroCuota - 1) meses (Tarjeta 4, CA2)
            val fechaCuota = sumarMeses(fechaPrimeraCuota, numeroCuota - 1)
            val (anio, mes) = parsearFecha(fechaCuota)

            Gasto(
                id              = generarId(),
                monto           = if (numeroCuota == cantCuotas) montoUltimaCuota
                else montoCuota,
                descripcion     = descripcion.trim(),
                categoriaId     = categoriaId,
                categoriaNombre = categoria.nombre,
                categoriaColor  = categoria.color,
                categoriaIcono  = categoria.icono,
                fecha           = fechaCuota,
                anio            = anio,
                mes             = mes,
                // Indicador "Cuota X/Y" (Tarjeta 4, CA3)
                cuotaActual     = numeroCuota,
                cuotaTotal      = cantCuotas,
                // ID padre que vincula toda la serie (Tarjeta 4, CA4)
                idPadre         = idPadre
            )
        }

        // Insertar toda la serie en una sola transacción de base de datos
        gastoRepository.insertarSerieCuotas(serie)
        return Resultado.Exito(serie)
    }

    // Redondea a 2 decimales para evitar problemas de punto flotante
    private fun redondear(valor: Double): Double =
        kotlin.math.round(valor * 100) / 100.0
}