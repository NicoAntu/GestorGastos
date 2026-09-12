# Task 5 Brief: expect `ExportarGuardado` + actual JVM (Desktop)

Extraído del plan: `D:\GestorGastos\docs\superpowers\plans\2026-09-10-exportar-xlsx.md` (Task 5, líneas 709-764). Este archivo es tu única fuente de requisitos; usa los valores exactos verbatim.

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/angel/gg/export/ExportarGuardado.kt`
- Create: `composeApp/src/jvmMain/kotlin/com/angel/gg/export/ExportarGuardado.jvm.kt`

**Interfaces:**
- Produces: `expect class ExportarGuardado { suspend fun guardar(nombreSugerido: String, bytes: ByteArray): Boolean }` (cancelar o sin destino → `false`).

> Gate de Android de esta tarea NO se pide: aún no existe el actual Android (Task 6).

## Paso previo: leer el patrón expect/actual del proyecto

El proyecto ya tiene un patrón expect/actual: `data/settings/SettingsStore.kt` (expect), `SettingsStore.jvm.kt` (jvmMain), `SettingsStore.android.kt` (androidMain). Léelos rápido para respetar el estilo de declaraciones y de ubicación de archivos.

## Step 1: Declarar la expect (commonMain)

`composeApp/src/commonMain/kotlin/com/angel/gg/export/ExportarGuardado.kt`:

```kotlin
package com.angel.gg.export

expect class ExportarGuardado {
    suspend fun guardar(nombreSugerido: String, bytes: ByteArray): Boolean
}
```

## Step 2: Implementar el actual JVM

`composeApp/src/jvmMain/kotlin/com/angel/gg/export/ExportarGuardado.jvm.kt`:

```kotlin
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
```

Notas:
- `kotlinx.coroutines.swing.Swing` (Dispatchers.Swing) ya está disponible en jvmMain vía la dependencia `kotlinx-coroutines-swing` existente en build.gradle.kts; cómpralo al compilar.
- El diálogo y la escritura usan runCatching → Boolean.

## Step 3: Gate de compilación JVM

Run: `.\gradlew.bat :composeApp:compileKotlinJvm`
Expected: BUILD SUCCESSFUL (Android compila en Task 6).

## Environment / Global Constraints

- Working directory: `D:\GestorGastos`. Work from there (workdir) para todos los comandos.
- IMPORTANT: Este proyecto NO es un repositorio git. No uses git ni commits.
- No agregar dependencias nuevas.
- Si requires tests: NO se piden tests para esta tarea (es UI de plataforma; el gate es compilar).

## Your Job

1. Lee el patrón existente (SettingsStore expect/actual).
2. Implementa Steps 1-2 exactamente.
3. Gate de compilación (Step 3).
4. Self-review: completeness, calidad, YAGNI.
5. Report.

## When You're in Over Your Head

Siempre está bien parar y reportar BLOCKED/NEEDS_CONTEXT con detalles. No adivines. (Ej: si `kotlinx.coroutines.swing.Swing` no estuviera disponible, reporta qué tienes disponible.)

## Report Format

Report file: `D:\GestorGastos\.superpowers\sdd\2026-09-10-exportar-xlsx\task-5-report.md`
Include: qué implementaste, output del gate, archivos cambiados, self-review findings, desviaciones.

Then report back with ONLY (under 15 lines):
- **Status:** DONE | DONE_WITH_CONCERNS | BLOCKED | NEEDS_CONTEXT
- One-line gate summary
- Your concerns, if any
- The report file path

If BLOCKED or NEEDS_CONTEXT, put the specifics in the final message itself.