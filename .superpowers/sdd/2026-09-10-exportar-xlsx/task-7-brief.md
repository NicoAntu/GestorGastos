# Task 7 Brief: ExportarViewModel + DI

Extraído del plan: `D:\GestorGastos\docs\superpowers\plans\2026-09-10-exportar-xlsx.md` (Task 7, líneas 888-1036). Este archivo es tu única fuente de requisitos; usa los valores exactos verbatim.

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/angel/gg/presentation/viewmodel/ExportarViewModel.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/angel/gg/di/AppModule.kt`

**Interfaces:**
- Consumes: `ExportarGastosUseCase` (Task 4), `XlsxGenerador` + `ExportarContenido` (Task 2), `ExportarGuardado` (Tasks 5-6), `Strings.t`/`Strings.mes`.
- Produces: `data class ExportarUiState(...)`; `class ExportarViewModel` con `state: StateFlow<ExportarUiState>`, `fun exportar(alcance: ExportarAlcance, desde: LocalDate?, hasta: LocalDate?)`, `fun guardar()`, `fun limpiarMensaje()`.

## Paso previo: leer el estado real

Los archivos consumidos ya existen (verificado):
- `ExportarGastosUseCase.ejecutar(alcance, desde, hasta): ExportarDatos` (Task 4).
- `XlsxGenerador.generar(contenido): ByteArray` (Task 2).
- `ExportarGuardado.guardar(nombreSugerido, bytes): Boolean` (expect commonMain, Tasks 5-6).
- `ExportarContenido(hoja, nombreArchivo, gastos, ingresoTotal)` (Task 2).
- `Strings.t(key)` no-composable y `Strings.mes(indice)` existen en `com.angel.gg.presentation.i18n`.
- AppModule.kt actual tiene 3 modules: `dataModule`, `domainModule` (factories de use cases), `viewModelModule` (factories/single de ViewModels). Los ViewModels del proyecto usan `androidx.lifecycle.ViewModel` + `viewModelScope` + `MutableStateFlow`/`StateFlow`/`update` (mira `EditarGastoViewModel.kt` o `EstadisticasViewModel.kt` para el patrón de imports).

## Step 1: Implementar el ViewModel

`composeApp/src/commonMain/kotlin/com/angel/gg/presentation/viewmodel/ExportarViewModel.kt`:

```kotlin
package com.angel.gg.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.angel.gg.domain.model.ExportarAlcance
import com.angel.gg.domain.model.ExportarContenido
import com.angel.gg.domain.usecase.ExportarGastosUseCase
import com.angel.gg.export.ExportarGuardado
import com.angel.gg.export.XlsxGenerador
import com.angel.gg.presentation.i18n.Strings
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ExportarUiState(
    val exportando: Boolean = false,
    val listoParaGuardar: Boolean = false,
    val bytes: ByteArray? = null,
    val nombreArchivo: String? = null,
    val mensaje: String? = null,
    val esError: Boolean = false
)

class ExportarViewModel(
    private val exportarGastosUseCase: ExportarGastosUseCase,
    private val exportarGuardado: ExportarGuardado
) : ViewModel() {

    private val _state = MutableStateFlow(ExportarUiState())
    val state: StateFlow<ExportarUiState> = _state

    fun exportar(alcance: ExportarAlcance, desde: LocalDate?, hasta: LocalDate?) {
        viewModelScope.launch {
            _state.update { it.copy(exportando = true, mensaje = null, bytes = null, listoParaGuardar = false) }
            runCatching { exportarGastosUseCase.ejecutar(alcance, desde, hasta) }
                .onSuccess { datos ->
                    if (datos.gastos.isEmpty()) {
                        _state.update {
                            it.copy(exportando = false, mensaje = Strings.t("exportar.sinDatos"), esError = true)
                        }
                    } else {
                        val contenido = ExportarContenido(
                            hoja = hojaDe(datos.alcance, datos),
                            nombreArchivo = archivoDe(datos.alcance, datos),
                            gastos = datos.gastos,
                            ingresoTotal = datos.ingresoTotal
                        )
                        val bytes = XlsxGenerador.generar(contenido)
                        _state.update {
                            it.copy(
                                exportando = false, bytes = bytes,
                                nombreArchivo = contenido.nombreArchivo, listoParaGuardar = true
                            )
                        }
                    }
                }
                .onFailure {
                    _state.update { it.copy(exportando = false, mensaje = Strings.t("exportar.error"), esError = true) }
                }
        }
    }

    fun guardar() {
        val actual = _state.value
        val bytes = actual.bytes ?: return
        val nombre = actual.nombreArchivo ?: return
        viewModelScope.launch {
            val ok = exportarGuardado.guardar(nombre, bytes)
            _state.update {
                it.copy(
                    listoParaGuardar = false, bytes = null, nombreArchivo = null,
                    mensaje = if (ok) Strings.t("exportar.exito") else null,
                    esError = !ok
                )
            }
        }
    }

    fun limpiarMensaje() {
        _state.update { it.copy(mensaje = null, esError = false) }
    }

    private fun hojaDe(alcance: ExportarAlcance, datos: com.angel.gg.domain.model.ExportarDatos): String =
        when (alcance) {
            ExportarAlcance.MES_ACTUAL -> "${Strings.mes(datos.desde.monthValue)} ${datos.desde.year}"
            ExportarAlcance.ANIO_ACTUAL -> datos.desde.year.toString()
            else -> "Gastos"
        }

    private fun archivoDe(alcance: ExportarAlcance, datos: com.angel.gg.domain.model.ExportarDatos): String =
        when (alcance) {
            ExportarAlcance.MES_ACTUAL -> "Gastos ${Strings.mes(datos.desde.monthValue)} ${datos.desde.year}.xlsx"
            ExportarAlcance.ANIO_ACTUAL -> "Gastos ${datos.desde.year}.xlsx"
            ExportarAlcance.TODO -> "Gastos.xlsx"
            ExportarAlcance.PERSONALIZADO -> "Gastos ${datos.desde} a ${datos.hasta}.xlsx"
        }
}
```

Notas:
- Importa `com.angel.gg.domain.model.ExportarDatos` limpio (usa el import en vez del fully-qualified si prefieres; el código del brief lo usa fully-qualified — si lo limpias, mantén coherencia).
- `Strings.mes(indice)` toma 1-based month (1=Enero). `datos.desde.monthValue` es 1-12. OK.

## Step 2: Registrar en DI (AppModule)

Añadir en `domainModule` (al final, después del factory de `GestionarCategoriaUseCase`):

```kotlin
    factory {
        ExportarGastosUseCase(
            gastoRepository = get(),
            ingresoMensualRepository = get()
        )
    }
```

Y en `viewModelModule` (al final, después del factory de `EditarGastoViewModel`):

```kotlin
    factory {
        ExportarViewModel(
            exportarGastosUseCase = get(),
            exportarGuardado = get()
        )
    }
```

Con los imports correspondientes en AppModule.kt:

```kotlin
import com.angel.gg.domain.usecase.ExportarGastosUseCase
import com.angel.gg.export.ExportarGuardado
import com.angel.gg.presentation.viewmodel.ExportarViewModel
```

(O los añades con el orden alfabético que sigue el archivo; respeta el estilo de alineación del archivo.)

## Step 3: Gate de compilación

Run: `.\gradlew.bat :composeApp:compileKotlinJvm :composeApp:compileDebugKotlinAndroid`
Expected: BUILD SUCCESSFUL.

## Environment / Global Constraints

- Working directory: `D:\GestorGastos`. Work from there (workdir) para todos los comandos.
- IMPORTANT: Este proyecto NO es un repositorio git. No uses git ni commits.
- No agregar dependencias nuevas (ViewModels ya dependen de lifecycle-viewmodel-compose existente).

## Your Job

1. Lee el patrón de un ViewModel existente (p.ej. `EditarGastoViewModel.kt`) para respetar estilo de imports.
2. Implementa Steps 1-2 exactamente.
3. Gate de compilación (Step 3).
4. Self-review: completeness (VM completo, beans en modules, imports), calidad (manejo de errores runCatching, flujo listoParaGuardar, limpieza de mensaje), YAGNI.
5. Report.

## When You're in Over Your Head

Siempre está bien parar y reportar BLOCKED/NEEDS_CONTEXT con detalles. No adivines.

## Report Format

Report file: `D:\GestorGastos\.superpowers\sdd\2026-09-10-exportar-xlsx\task-7-report.md`
Include: qué implementaste, output del gate, archivos cambiados, self-review findings, desviaciones.

Then report back with ONLY (under 15 lines):
- **Status:** DONE | DONE_WITH_CONCERNS | BLOCKED | NEEDS_CONTEXT
- One-line gate summary
- Your concerns, if any
- The report file path

If BLOCKED or NEEDS_CONTEXT, put the specifics in the final message itself.