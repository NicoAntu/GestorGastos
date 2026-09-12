package com.angel.gg

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.resume
import com.arkivanov.essenty.lifecycle.stop
import com.angel.gg.di.desktopModule
import com.angel.gg.di.initKoin
import com.angel.gg.presentation.navigation.RootComponent
import java.awt.EventQueue

fun main() {
    initKoin(platformModule = desktopModule)

    // Decompose requiere que el ComponentContext se cree en el hilo de UI (EDT)
    val lifecycle = LifecycleRegistry()

    val root = invokeOnMainThread {
        RootComponent(DefaultComponentContext(lifecycle))
    }

    application {
        lifecycle.resume()

        Window(
            onCloseRequest = {
                lifecycle.stop()
                exitApplication()
            },
            title = "GestorGastos"
        ) {
            App(root = root)
        }
    }
}

// Ejecuta un bloque en el Event Dispatch Thread y devuelve el resultado
private fun <T> invokeOnMainThread(block: () -> T): T {
    var result: T? = null
    if (EventQueue.isDispatchThread()) {
        result = block()
    } else {
        EventQueue.invokeAndWait { result = block() }
    }
    @Suppress("UNCHECKED_CAST")
    return result as T
}