package com.angel.gg.export

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Puente entre el launcher de SAF (registrado en MainActivity) y el suspend leer
object ImportarPuente {
    private var launcher: (() -> Unit)? = null
    private var contexto: Context? = null
    private var pendiente: CompletableDeferred<Uri?>? = null

    fun registrar(lanzar: () -> Unit, ctx: Context) {
        launcher = lanzar
        contexto = ctx
    }

    fun resolver(uri: Uri?) {
        pendiente?.complete(uri)
        pendiente = null
    }

    suspend fun leer(): ByteArray? {
        launcher ?: return null
        val d = CompletableDeferred<Uri?>()
        pendiente = d
        launcher?.invoke()
        val uri = d.await() ?: return null
        val ctx = contexto ?: return null
        return withContext(Dispatchers.IO) {
            runCatching { ctx.contentResolver.openInputStream(uri)?.use { it.readBytes() } }.getOrNull()
        }
    }
}

actual class ImportarLectura {
    actual suspend fun leer(): ByteArray? = ImportarPuente.leer()
}