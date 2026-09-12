package com.angel.gg.export

import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.swing.Swing

actual class ImportarLectura {
    actual suspend fun leer(): ByteArray? =
        withContext(Dispatchers.Swing) {
            val chooser = JFileChooser().apply {
                dialogTitle = "Importar xlsx"
                fileFilter = FileNameExtensionFilter("Excel (.xlsx)", "xlsx")
            }
            if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                runCatching { chooser.selectedFile.readBytes() }.getOrNull()
            } else null
        }
}