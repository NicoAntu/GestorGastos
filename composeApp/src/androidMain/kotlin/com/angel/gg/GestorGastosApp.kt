package com.angel.gg

import android.app.Application
import com.angel.gg.di.androidModule
import com.angel.gg.di.dataModule
import com.angel.gg.di.domainModule
import com.angel.gg.di.viewModelModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class GestorGastosApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@GestorGastosApp)  // ← esto era lo que faltaba
            modules(
                androidModule,
                dataModule,
                domainModule,
                viewModelModule
            )
        }
    }
}