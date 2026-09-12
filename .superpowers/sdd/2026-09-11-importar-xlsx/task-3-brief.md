### Task 3: Lectura de archivo (expect/actual)

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/angel/gg/export/ImportarLectura.kt`
- Create: `composeApp/src/jvmMain/kotlin/com/angel/gg/export/ImportarLectura.jvm.kt`
- Create: `composeApp/src/androidMain/kotlin/com/angel/gg/export/ImportarLectura.android.kt`
- Modify: `composeApp/src/androidMain/kotlin/com/angel/gg/MainActivity.kt`

**Interfaces:**
- Consumes: patrones de `ExportarGuardado` (`expect class` + actuals, puente SAF en Android).
- Produces: `expect class ImportarLectura { suspend fun leer(): ByteArray? }` — `null` si el usuario cancela.

- [ ] **Step 1: Create commonMain expect**

`ImportarLectura.kt`:

```kotlin
package com.angel.gg.export

expect class ImportarLectura {
    suspend fun leer(): ByteArray?
}
```

- [ ] **Step 2: Create jvmMain actual**

`ImportarLectura.jvm.kt`:

```kotlin
package com.angel.gg.export

import java.io.File
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
```

- [ ] **Step 3: Create androidMain actual**

`ImportarLectura.android.kt`:

```kotlin
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
```

- [ ] **Step 4: Registro del launcher en MainActivity (androidMain)**

`MainActivity.kt` — añadir junto al bloque `ExportarPuente` existente (mantenerlo intacto). Añadir el import con los demás `com.angel.gg.*` (tras línea 10), el campo junto a `crearDocumento` (tras línea 19), y el registro en `onCreate` justo después de `ExportarPuente.registrar(...)` (línea 23):

```kotlin
import com.angel.gg.export.ImportarPuente
...
    private val abrirDocumento = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> ImportarPuente.resolver(uri) }
...
        ImportarPuente.registrar(
            { abrirDocumento.launch(arrayOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) },
            applicationContext
        )
```

- [ ] **Step 5: Gates**

Run (workdir `D:\GestorGastos`): `.\gradlew.bat :composeApp:jvmTest :composeApp:compileKotlinJvm :composeApp:compileDebugKotlinAndroid`
Expected: tests PASS y BUILD SUCCESSFUL (incluye Android con el nuevo launcher).

---

