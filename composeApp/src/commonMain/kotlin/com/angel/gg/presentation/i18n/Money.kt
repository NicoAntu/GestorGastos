package com.angel.gg.presentation.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.angel.gg.data.settings.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object Money {

    @Volatile
    private var repo: SettingsRepository? = null

    private val MONEDA_DEFAULT = "$"
    private val monedaVacia    = MutableStateFlow(MONEDA_DEFAULT)

    fun init(r: SettingsRepository) {
        repo = r
    }

    val MONEDAS = listOf(
        "$", "US$", "€", "£", "¥", "R$", "S/", "B₴", "RN$", "៛", "W", "₩", "₱", "ƒ", "lei"
    )

    val currencyState: StateFlow<String> get() = repo?.currency ?: monedaVacia

    fun currencyNow(): String = repo?.currency?.value ?: MONEDA_DEFAULT

    fun setCurrency(simbolo: String) {
        repo?.setCurrency(simbolo)
    }

    @Composable
    fun format(monto: Double): String {
        val simbolo = currencyState.collectAsState().value
        return formatear(monto, simbolo)
    }

    @Composable
    fun formatConSigno(monto: Double): String {
        val texto = format(monto)
        return if (monto < 0) "-$texto" else texto
    }

    private fun formatear(monto: Double, simbolo: String): String {
        val abs = kotlin.math.abs(monto)
        val numero = if (abs >= 1000) {
            "%.0f".format(abs).reversed().chunked(3).joinToString(".").reversed()
        } else {
            "%.2f".format(abs)
        }
        return simbolo + numero
    }
}