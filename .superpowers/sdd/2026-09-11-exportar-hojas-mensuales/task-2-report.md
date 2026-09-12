# Task 2 Report: Fix DatePicker del rango Personalizado (FechaCampo)

## Status: DONE

## Qué implementé

Reemplazé el pattern roto `Box(Modifier.clickable)` + `OutlinedTextField(readOnly=true)` en `FechaCampo` de `ExportarDialog.kt` (el textfield consumía el click y `onClick` nunca se disparaba) por el patrón `Surface(onClick=…)` verbatim del brief, idéntico al de RegistrarGasto/EditarGasto.

### Archivos cambiados
- `composeApp/src/commonMain/kotlin/com/angel/gg/presentation/screen/ExportarDialog.kt`
  - **Imports:** quité `androidx.compose.foundation.clickable`; agregué `androidx.compose.foundation.BorderStroke` y `androidx.compose.foundation.shape.RoundedCornerShape`.
  - **Deviación menor (justificada):** quité también `import androidx.compose.foundation.layout.Box`, que quedó sin uso tras el reemplazo. Las releases anteriores de este brief requerían "sin warnings de imports sin usar" en los gates; mantener `Box` habría generado un warning de import sin usar. No hubo otros cambios.
  - **Body de `FechaCampo`:** reemplazado por el bloque verbatim del brief (`Surface(onClick...)` con `RoundedCornerShape(4.dp)`, `color = AppTheme.CardDark`, `border = BorderStroke(1.dp, AppTheme.BorderColor)`, `Row` con `Text` + `Icon(AppIcons.Calendario)`, `tr("registrar.seleccionarFecha")` como placeholder). Firma intacta — los call sites del diálogo NO cambiaron.
  - No agregué comentarios ni claves i18n nuevas.

## Gates

### Gate 1: `.\gradlew.bat :composeApp:jvmTest`
```
BUILD SUCCESSFUL in 8s
16 actionable tasks: 4 executed, 12 up-to-date
```
(últimas líneas del output; `:composeApp:jvmTest` ejecutado, PASS. Sin warnings en `ExportarDialog.kt`; los warnings pre-existentes son de `expect`/`actual` Beta y `menuAnchor()` deprecated en Registrar/EditarGasto.)

### Gate 2: `.\gradlew.bat :composeApp:compileKotlinJvm :composeApp:compileDebugKotlinAndroid`
```
BUILD SUCCESSFUL in 7s
25 actionable tasks: 2 executed, 23 up-to-date
```
(`:composeApp:compileKotlinJvm` y `:composeApp:compileDebugKotlinAndroid` ambos OK. Sin warnings en `ExportarDialog.kt`; restantes pre-existentes.)

## Verificación manual (Step 4)
NO ejecutada: entorno de agente sin GUI interactivo. Pendiente de verificación humana (Ajustes → Exportar → alcance "Personalizado" → clic en Desde/Hasta → debe abrirse el `DatePickerDialog`).

## Devisiones del brief
1. Remoción del import `Box` (ver "Qué implementé") — necesaria para cumplir el criterio de gates sin warnings de imports sin usar.
2. El `@OptIn(ExperimentalMaterial3Api::class)` del brief en `FechaCampo` se aplicó como está escrito (ya era necesario a nivel archivo).