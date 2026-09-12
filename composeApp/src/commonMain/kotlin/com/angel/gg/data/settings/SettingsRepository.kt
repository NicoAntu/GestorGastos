package com.angel.gg.data.settings

import com.angel.gg.presentation.i18n.Lang
import com.angel.gg.presentation.i18n.Money
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsRepository(private val store: SettingsStore) {

    private val _lang = MutableStateFlow(parseLang(store.getLang()))
    val lang: StateFlow<Lang> = _lang.asStateFlow()

    private val _currency = MutableStateFlow(store.getCurrency().takeIf { it in Money.MONEDAS } ?: "$")
    val currency: StateFlow<String> = _currency.asStateFlow()

    fun setLang(lang: Lang) {
        _lang.value = lang
        store.setLang(lang.name)
    }

    fun setCurrency(currency: String) {
        _currency.value = currency
        store.setCurrency(currency)
    }

    private fun parseLang(raw: String): Lang =
        Lang.entries.firstOrNull { it.name == raw } ?: Lang.ES
}