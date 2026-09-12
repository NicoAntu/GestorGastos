# Task 6 Brief: actual Android (SAF) + registro en MainActivity + DI

Extraído del plan: `D:\GestorGastos\docs\superpowers\plans\2026-09-10-exportar-xlsx.md` (Task 6, líneas 767-884). Este archivo es tu única fuente de requisitos; usa los valores exactos verbatim.

**Files:**
- Create: `composeApp/src/androidMain/kotlin/com/angel/gg/export/ExportarGuardado.android.kt`
- Modify: `composeApp/src/androidMain/kotlin/com/angel/gg/MainActivity.kt`
- Modify: `composeApp/src/androidMain/kotlin/com/angel/gg/di/AndroidModule.kt`
- Modify: `composeApp/src/jvmMain/kotlin/com/angel/gg/di/DesktopModule.kt`

**Interfaces:**
- Consumes: `expect class ExportarGuardado` (Task 5, ya existe en commonMain).
- Produces: bean Koin `single<ExportarGuardado> { ExportarGuardado() }` en `androidModule` y `desktopModule`.

## Paso previo: leer el estado real

Leo ya el estado actual de los archivos destino para ti (verificado):
- `MainActivity.kt` actual: `class MainActivity : ComponentActivity()` con `onCreate` que crea `RootComponent(defaultComponentContext())` y `setContent { App(root = root) }`. Imports actuales: Bundle, ComponentActivity, setContent, defaultComponentContext, androidModule, initKoin, RootComponent. (Nota: `context` para el puente debe ser el `applicationContext` o `this`; el pattern del proyecto es bean Koin con `androidContext()`.)
- `AndroidModule.kt` actual: module con `DatabaseDriverFactory(androidContext())` y `SettingsStore(androidContext())`.
- `DesktopModule.kt` actual: module con `DatabaseDriverFactory()` y `SettingsStore()`.

## Step 1: Implementar el actual Android + puente

`composeApp/src/androidMain/kotlin/com/angel/gg/export/ExportarGuardado.android.kt`:

```kotlin
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
```

## Step 2: Registrar el launcher en MainActivity

Modificar `MainActivity.kt` para que quede así (manteniendo el `onCreate` actual):

```kotlin
package com.angel.gg

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import com.arkivanov.decompose.defaultComponentContext
import com.angel.gg.di.androidModule
import com.angel.gg.di.initKoin
import com.angel.gg.export.ExportarPuente
import com.angel.gg.presentation.navigation.RootComponent

class MainActivity : ComponentActivity() {

    private val crearDocumento = registerForActivityResult(
        ActivityResultContracts.CreateDocument(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        )
    ) { uri -> ExportarPuente.resolver(uri) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ExportarPuente.registrar({ crearDocumento.launch(it) }, applicationContext)

        val root = RootComponent(defaultComponentContext())

        setContent {
            App(root = root)
        }
    }
}
```

## Step 3: Registrar el bean de plataforma en DI

AndroidModule — añadir dentro del module:

```kotlin
    single<ExportarGuardado> { ExportarGuardado() }
```

con import `com.angel.gg.export.ExportarGuardado`.

DesktopModule — añadir dentro del module:

```kotlin
    single<ExportarGuardado> { ExportarGuardado() }
```

con import `com.angel.gg.export.ExportarGuardado`.

## Step 4: Gate de compilación completo

Run: `.\gradlew.bat :composeApp:compileKotlinJvm :composeApp:compileDebugKotlinAndroid`
Expected: BUILD SUCCESSFUL. (Ahora ambas plataformas compilan con todas las Tasks 1-6.)

## Environment / Global Constraints

- Working directory: `D:\GestorGastos`. Work from there (workdir) para todos los comandos.
- IMPORTANT: Este proyecto NO es un repositorio git. No uses git ni commits.
- No agregar dependencias nuevas.
- El `initKoin`/`androidModule` ya existen en los imports actuales de MainActivity; mantén el resto del archivo intacto salvo los cambios indicados. Date cuenta de que `initKoin` se invoca en el arranque de la app (probablemente en `App`/init de plataforma); NO lo toques.

## Your Job

1. Implementa Steps 1-3 exactamente.
2. Gate de compilación (Step 4).
3. Self-review: completeness (archivos, beans Koin en ambos modules), calidad (thread-safety del puente: `CompletableDeferred.await` se lanza en el coroutine del VM; `openOutputStream` en Dispatchers.IO), YAGNI.
4. Report.

## When You're in Over Your Head

Siempre está bien parar y reportar BLOCKED/NEEDS_CONTEXT con detalles. No adivines. (P.ej. si `ActivityResultContracts.CreateDocument` requiere otro import o si `androidContext()` en el module ya existiera.)

## Report Format

Report file: `D:\GestorGastos\.superpowers\sdd\2026-09-10-exportar-xlsx\task-6-report.md`
Include: qué implementaste, output del gate, archivos cambiados, self-review findings, desviaciones.

Then report back with ONLY (under 15 lines):
- **Status:** DONE | DONE_WITH_CONCERNS | BLOCKED | NEEDS_CONTEXT
- One-line gate summary
- Your concerns, if any
- The report file path

If BLOCKED or NEEDS_CONTEXT, put the specifics in the final message itself.