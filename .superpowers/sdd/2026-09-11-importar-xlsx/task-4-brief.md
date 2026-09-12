### Task 4: UI + i18n + DI

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/angel/gg/presentation/viewmodel/ImportarViewModel.kt`
- Create: `composeApp/src/commonMain/kotlin/com/angel/gg/presentation/screen/ImportarDialog.kt`
- Modify: `composeApp/src/androidMain/kotlin/com/angel/gg/di/AndroidModule.kt`
- Modify: `composeApp/src/jvmMain/kotlin/com/angel/gg/di/DesktopModule.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/angel/gg/di/AppModule.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/angel/gg/presentation/i18n/EsStrings.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/angel/gg/presentation/i18n/EnStrings.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/angel/gg/presentation/i18n/FrStrings.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/angel/gg/presentation/screen/AjustesScreen.kt`

**Interfaces:**
- Consumes (Tasks 1–3): `ImportarXlsx.leer(bytes): ResultadoLectura { Ok(datos: ResultadoImportacion) | ArchivoInvalido }`, `ImportarLectura.leer(): ByteArray?`, `ImportarGastosUseCase(datos): ImportacionResultado { Exito(resumen) | SinGastos }`. Existentes: `Strings.t(key, vararg args)` con placeholders `{0}`…, `tr(key)`, `AppTheme.*`, `AppIcons.Importar`, `koinInject()`.
- Produces: `class ImportarViewModel(importarLectura, importarGastosUseCase)` con `state: StateFlow<ImportarUiState>` y `importar()`/`limpiarMensaje()`; `ImportarDialog(onDismiss, viewModel = koinInject())`.

- [ ] **Step 1: i18n — añadir claves `importar.*` (ES/EN/FR)**

En `EsStrings.kt`, justo después de la entrada `"exportar.sinDatos"` (mantener el bloque `exportar.*` completo):

```kotlin
        "importar.titulo"          to "Importar gastos",
        "importar.boton"           to "Seleccionar archivo",
        "importar.exito"           to "Se importaron {0} gastos ({1} ya existían, {2} categorías creadas, {3} ingresos)",
        "importar.error"           to "No se pudo importar",
        "importar.archivoNoValido" to "El archivo no es un xlsx válido",
        "importar.sinGastos"       to "El archivo no contiene gastos",
```

En `EnStrings.kt`, análogo (guardar el bloque `exportar.*` intacto):

```kotlin
        "importar.titulo"          to "Import expenses",
        "importar.boton"           to "Select file",
        "importar.exito"           to "Imported {0} expenses ({1} already existed, {2} categories created, {3} incomes)",
        "importar.error"           to "Import failed",
        "importar.archivoNoValido" to "The file is not a valid xlsx",
        "importar.sinGastos"       to "The file contains no expenses",
```

En `FrStrings.kt`, análogo (guardar el bloque `exportar.*` intacto):

```kotlin
        "importar.titulo"          to "Importer des dépenses",
        "importar.boton"           to "Choisir un fichier",
        "importar.exito"           to "{0} dépenses importées ({1} existaient déjà, {2} catégories créées, {3} revenus)",
        "importar.error"           to "Échec de l'importation",
        "importar.archivoNoValido" to "Le fichier n'est pas un xlsx valide",
        "importar.sinGastos"       to "Le fichier ne contient aucune dépense",
```

- [ ] **Step 2: Create `presentation/viewmodel/ImportarViewModel.kt`**

```kotlin
package com.angel.gg.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.angel.gg.domain.model.ImportacionResultado
import com.angel.gg.domain.usecase.ImportarGastosUseCase
import com.angel.gg.export.ImportarLectura
import com.angel.gg.export.ImportarXlsx
import com.angel.gg.presentation.i18n.Strings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ImportarUiState(
    val importando: Boolean = false,
    val mensaje: String? = null,
    val esError: Boolean = false
)

class ImportarViewModel(
    private val importarLectura: ImportarLectura,
    private val importarGastosUseCase: ImportarGastosUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(ImportarUiState())
    val state: StateFlow<ImportarUiState> = _state

    fun importar() {
        viewModelScope.launch {
            _state.update { it.copy(importando = true, mensaje = null) }
            runCatching {
                val bytes = importarLectura.leer() ?: run {
                    _state.update { it.copy(importando = false) }
                    return@launch
                }
                when (val lectura = ImportarXlsx.leer(bytes)) {
                    is ImportarXlsx.ResultadoLectura.ArchivoInvalido ->
                        _state.update {
                            it.copy(importando = false, mensaje = Strings.t("importar.archivoNoValido"), esError = true)
                        }
                    is ImportarXlsx.ResultadoLectura.Ok ->
                        when (val resultado = importarGastosUseCase(lectura.datos)) {
                            is ImportacionResultado.SinGastos ->
                                _state.update {
                                    it.copy(importando = false, mensaje = Strings.t("importar.sinGastos"), esError = true)
                                }
                            is ImportacionResultado.Exito ->
                                _state.update {
                                    it.copy(
                                        importando = false,
                                        mensaje = Strings.t(
                                            "importar.exito",
                                            resultado.resumen.nuevos,
                                            resultado.resumen.omitidos,
                                            resultado.resumen.categoriasCreadas,
                                            resultado.resumen.ingresosRestaurados
                                        ),
                                        esError = false
                                    )
                                }
                        }
                }
            }.onFailure {
                _state.update { it.copy(importando = false, mensaje = Strings.t("importar.error"), esError = true) }
            }
        }
    }

    fun limpiarMensaje() {
        _state.update { it.copy(mensaje = null, esError = false) }
    }
}
```

- [ ] **Step 3: Create `presentation/screen/ImportarDialog.kt`**

```kotlin
package com.angel.gg.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.angel.gg.presentation.AppTheme
import com.angel.gg.presentation.i18n.tr
import com.angel.gg.presentation.viewmodel.ImportarViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportarDialog(
    onDismiss: () -> Unit,
    viewModel: ImportarViewModel = koinInject()
) {
    val estado by viewModel.state.collectAsState()

    // Cerrar el diálogo al confirmar la importación (la snackbar muestra el resumen)
    LaunchedEffect(estado.mensaje) {
        val m = estado.mensaje
        if (m != null && !estado.esError) onDismiss()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppTheme.CardDark,
        title = { Text(tr("importar.titulo"), color = AppTheme.TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(tr("ajustes.importarSub"), color = AppTheme.TextMuted, fontSize = 13.sp)
                if (estado.importando) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.padding(8.dp), color = AppTheme.Indigo)
                    }
                }
                estado.mensaje?.let {
                    if (estado.esError) {
                        Text(it, color = AppTheme.Red, fontSize = 13.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !estado.importando,
                onClick = { viewModel.importar() }
            ) {
                Text(tr("importar.boton"), color = if (estado.importando) AppTheme.TextSubtle else AppTheme.Indigo)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(tr("comun.cancelar"), color = AppTheme.TextMuted)
            }
        }
    )
}
```

- [ ] **Step 4: DI — módulos de plataforma + AppModule**

**`AndroidModule.kt`** (junto a las otras líneas `single`, conservar el resto):

```kotlin
import com.angel.gg.export.ImportarLectura
...
    single<ImportarLectura> { ImportarLectura() }
```

**`DesktopModule.kt`** (análogo):

```kotlin
import com.angel.gg.export.ImportarLectura
...
    single<ImportarLectura> { ImportarLectura() }
```

**`AppModule.kt`** — añadir imports (no eliminar los existentes):

```kotlin
import com.angel.gg.domain.usecase.ImportarGastosUseCase
import com.angel.gg.presentation.viewmodel.ImportarViewModel
```

En `domainModule`, después del bloque de `ExportarGastosUseCase` (líneas 124-129, antes del cierre en línea 130):

```kotlin
    factory {
        ImportarGastosUseCase(
            gastoRepository           = get(),
            categoriaRepository       = get(),
            ingresoMensualRepository  = get(),
            gestionarCategoriaUseCase = get()
        )
    }
```

En `viewModelModule`, después del bloque de `ExportarViewModel` (líneas 194-199, antes del cierre en línea 200):

```kotlin
    factory {
        ImportarViewModel(
            importarLectura       = get(),
            importarGastosUseCase = get()
        )
    }
```

- [ ] **Step 5: Wire AjustesScreen**

`AjustesScreen.kt` (mantener bloque de exportación intacto):
- Añadir import: `import com.angel.gg.presentation.viewmodel.ImportarViewModel`
- Añadir estado y VM junto a los del bloque de exportar (líneas 40-42):

```kotlin
    var abrirImportar by remember { mutableStateOf(false) }
    val importarViewModel: ImportarViewModel = koinInject()
    val importarEstado by importarViewModel.state.collectAsState()
```

- Añadir efecto de snackbar justo después del `LaunchedEffect(exportarEstado.mensaje)` (líneas 45-50):

```kotlin
    LaunchedEffect(importarEstado.mensaje) {
        importarEstado.mensaje?.let {
            snackbarHostState.showSnackbar(it)
            importarViewModel.limpiarMensaje()
        }
    }
```

- Conectar la fila (AjustesScreen.kt:162-165): cambiar `onClick = {}` por `onClick = { abrirImportar = true }`.
- Añadir el diálogo junto al de exportar (después del bloque `if (abrirExportar) {...}` de líneas 175-180, dentro del `Scaffold`):

```kotlin
        if (abrirImportar) {
            ImportarDialog(
                onDismiss = { abrirImportar = false },
                viewModel = importarViewModel
            )
        }
```

- [ ] **Step 6: Gates**

Run (workdir `D:\GestorGastos`): `.\gradlew.bat :composeApp:jvmTest :composeApp:compileKotlinJvm :composeApp:compileDebugKotlinAndroid`
Expected: tests PASS y BUILD SUCCESSFUL (sin warnings de imports sin usar).

- [ ] **Step 7: Verificación manual (desktop, solo si hay GUI; si no, dejarlo pendiente para el humano)**

Run (workdir `D:\GestorGastos`): `.\gradlew.bat :composeApp:run`
Ajustes → Importar → seleccionar un .xlsx **exportado** por la app → debe aparecer el resumen en la snackbar; los gastos deben totalizarse en sus meses; importar el archivo de nuevo debe omitir todo (0 nuevos). Archivo corrupto → mensaje "no es un xlsx válido".
