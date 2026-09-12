package com.angel.gg

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import com.arkivanov.decompose.defaultComponentContext
import com.angel.gg.di.androidModule
import com.angel.gg.di.initKoin
import com.angel.gg.export.ExportarPuente
import com.angel.gg.export.ImportarPuente
import com.angel.gg.presentation.navigation.RootComponent

class MainActivity : ComponentActivity() {

    private val crearDocumento = registerForActivityResult(
        ActivityResultContracts.CreateDocument(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        )
    ) { uri -> ExportarPuente.resolver(uri) }

    private val abrirDocumento = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> ImportarPuente.resolver(uri) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ExportarPuente.registrar({ crearDocumento.launch(it) }, applicationContext)
        ImportarPuente.registrar(
            { abrirDocumento.launch(arrayOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) },
            applicationContext
        )

        val root = RootComponent(defaultComponentContext())

        setContent {
            App(root = root)
        }
    }
}
