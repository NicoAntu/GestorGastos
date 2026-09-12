package com.angel.gg.data.settings

expect class SettingsStore {
    fun getLang(): String
    fun setLang(lang: String)
    fun getCurrency(): String
    fun setCurrency(currency: String)
}