package com.angel.gg.di

import org.koin.core.context.startKoin
import org.koin.core.module.Module

fun initKoin(platformModule: Module) {
    startKoin {
        modules(
            platformModule,
            dataModule,
            domainModule,
            viewModelModule
        )
    }
}