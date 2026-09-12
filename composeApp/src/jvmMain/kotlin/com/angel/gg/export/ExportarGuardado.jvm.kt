package com.angel.gg.export

import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.swing.Swing

actual class ExportarGuardado {
    actual suspend fun guardar(nombreSugerido: String, bytes: ByteArray): Boolean =
        withContext(Dispatchers.Swing) {
            val chooser = JFileChooser().apply {
                dialogTitle = "Exportar xlsx"
                selectedFile = File(nombreSugerido)
                fileFilter = FileNameExtensionFilter("Excel (.xlsx)", "xlsx")
            }
            if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                val destino = chooser.selectedFile
                val archivo = if (destino.name.endsWith(".xlsx", ignoreCase = true))
                    destino else File(destino.parentFile, "${destino.name}.xlsx")
                runCatching { archivo.writeBytes(bytes) }.isSuccess
            } else false
        }
}