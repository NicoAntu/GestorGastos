package com.angel.gg.presentation.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.angel.gg.data.settings.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class Lang(val etiqueta: String) {
    ES("Español"),
    EN("English"),
    FR("Français")
}

object Strings {

    @Volatile
    private var repo: SettingsRepository? = null

    private val langVacio = MutableStateFlow(Lang.ES)

    fun init(r: SettingsRepository) {
        repo = r
    }

    val langState: StateFlow<Lang> get() = repo?.lang ?: langVacio

    fun langNow(): Lang = repo?.lang?.value ?: Lang.ES

    fun setLang(lang: Lang) {
        repo?.setLang(lang)
    }

    @Composable
    fun tr(key: String): String {
        val lang = langState.collectAsState().value
        return resolve(key, lang)
    }

    fun t(key: String): String = resolve(key, langNow())

    @Composable
    fun tr(key: String, vararg args: Any?): String {
        val lang = langState.collectAsState().value
        return rellenar(resolve(key, lang), args)
    }

    fun t(key: String, vararg args: Any?): String = rellenar(resolve(key, langNow()), args)

    private fun resolve(key: String, lang: Lang): String =
        when (lang) {
            Lang.ES -> EsStrings.map[key]
            Lang.EN -> EnStrings.map[key]
            Lang.FR -> FrStrings.map[key]
        }?.takeIf { it.isNotEmpty() } ?: EsStrings.map[key] ?: key

    private fun rellenar(plantilla: String, args: Array<out Any?>): String {
        if (args.isEmpty()) return plantilla
        var resultado = plantilla
        args.forEachIndexed { i, arg ->
            resultado = resultado.replace("{$i}", arg?.toString() ?: "")
        }
        return resultado
    }

    fun meses(lang: Lang): List<String> = when (lang) {
        Lang.ES -> EsStrings.meses
        Lang.EN -> EnStrings.meses
        Lang.FR -> FrStrings.meses
    }

    fun mes(indice: Int): String = meses(langNow()).getOrElse(indice - 1) { "" }
}

@Composable
fun tr(key: String): String = Strings.tr(key)

@Composable
fun tr(key: String, vararg args: Any?): String = Strings.tr(key, *args)