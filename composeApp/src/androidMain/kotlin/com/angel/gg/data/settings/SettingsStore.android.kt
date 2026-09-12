package com.angel.gg.data.settings

import android.content.Context

actual class SettingsStore(private val context: Context) {

    private val prefs = context.getSharedPreferences("ajustes", Context.MODE_PRIVATE)

    actual fun getLang(): String = prefs.getString("idioma", "ES") ?: "ES"

    actual fun setLang(lang: String) {
        prefs.edit().putString("idioma", lang).apply()
    }

    actual fun getCurrency(): String = prefs.getString("moneda", "$") ?: "$"

    actual fun setCurrency(currency: String) {
        prefs.edit().putString("moneda", currency).apply()
    }
}