package com.angel.gg.di

import com.angel.gg.data.db.DatabaseDriverFactory
import com.angel.gg.data.settings.SettingsStore
import com.angel.gg.export.ExportarGuardado
import com.angel.gg.export.ImportarLectura
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val androidModule = module {
    single<DatabaseDriverFactory> { DatabaseDriverFactory(androidContext()) }
    single<SettingsStore> { SettingsStore(androidContext()) }
    single<ExportarGuardado> { ExportarGuardado() }
    single<ImportarLectura> { ImportarLectura() }
}
