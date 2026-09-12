package com.angel.gg.data.settings

import java.io.File
import java.util.Properties

actual class SettingsStore {

    private val file  = File(System.getProperty("user.home"), ".gestorgastos" + File.separator + "ajustes.properties")
    private val props = Properties().apply {
        if (file.exists()) file.inputStream().use { load(it) }
    }

    private fun save() {
        file.parentFile.mkdirs()
        file.outputStream().use { props.store(it, "Gestor Gastos") }
    }

    actual fun getLang(): String = props.getProperty("idioma", "ES")

    actual fun setLang(lang: String) {
        props.setProperty("idioma", lang)
        save()
    }

    actual fun getCurrency(): String = props.getProperty("moneda", "$")

    actual fun setCurrency(currency: String) {
        props.setProperty("moneda", currency)
        save()
    }
}