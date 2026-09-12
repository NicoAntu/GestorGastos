package com.angel.gg.export

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Puente entre el launcher de SAF (registrado en MainActivity) y el suspend guardar
object ExportarPuente {
    private var launcher: ((String) -> Unit)? = null
    private var contexto: Context? = null
    private var pendiente: CompletableDeferred<Uri?>? = null

    fun registrar(lanzar: (String) -> Unit, ctx: Context) {
        launcher = lanzar
        contexto = ctx
    }

    fun resolver(uri: Uri?) {
        pendiente?.complete(uri)
        pendiente = null
    }

    suspend fun guardar(nombreSugerido: String, bytes: ByteArray): Boolean {
        launcher ?: return false
        val d = CompletableDeferred<Uri?>()
        pendiente = d
        launcher?.invoke(nombreSugerido)
        val uri = d.await() ?: return false
        val ctx = contexto ?: return false
        return withContext(Dispatchers.IO) {
            try {
                ctx.contentResolver.openOutputStream(uri)?.use { it.write(bytes) } != null
            } catch (e: Exception) {
                false
            }
        }
    }
}

actual class ExportarGuardado {
    actual suspend fun guardar(nombreSugerido: String, bytes: ByteArray): Boolean =
        ExportarPuente.guardar(nombreSugerido, bytes)
}
