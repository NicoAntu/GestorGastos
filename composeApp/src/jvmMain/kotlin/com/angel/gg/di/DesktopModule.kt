package com.angel.gg.di

import com.angel.gg.data.db.DatabaseDriverFactory
import com.angel.gg.data.settings.SettingsStore
import com.angel.gg.export.ExportarGuardado
import com.angel.gg.export.ImportarLectura
import org.koin.dsl.module

val desktopModule = module {
    single<DatabaseDriverFactory> { DatabaseDriverFactory() }
    single<SettingsStore> { SettingsStore() }
    single<ExportarGuardado> { ExportarGuardado() }
    single<ImportarLectura> { ImportarLectura() }
}
