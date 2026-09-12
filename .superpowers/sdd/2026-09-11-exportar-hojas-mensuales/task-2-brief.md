# Task 2 Brief: Fix DatePicker del rango Personalizado (FechaCampo)

Extraído de: `D:\GestorGastos\docs\superpowers\plans\2026-09-11-exportar-hojas-mensuales.md` (Task 2). Uso el contenido exacto verbatim (pasos 1-4 abajo).

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/angel/gg/presentation/screen/ExportarDialog.kt`

**Interfaces:**
- Consumes: `AppTheme.CardDark/BorderColor/TextMuted/TextSubtle/TextPrimary`, `AppIcons.Calendario` (mismo paquete, ya existe), `tr("registrar.seleccionarFecha")` (clave i18n ya existente — NO agregar claves nuevas), `Surface(onClick=…)` M3.
- Produces: `FechaCampo(etiqueta, valor, modifier, onClick)` con la misma firma (los call sites del diálogo NO cambian).

## Notas de contexto

- El proyecto NO es un repositorio git: no usar git ni commits. Verificar con gates.
- No agregar dependencias nuevas.
- Este es un reemplazo localizado de la función `FechaCampo` privada (body líneas ~155-180). No tocar el resto del archivo (ni el diálogo, ni el wiring, ni el ViewModel).
- Motivo del fix: el patrón actual `Box(Modifier.clickable)` envolviendo un `OutlinedTextField(readOnly=true)` hace que el textfield consuma el click y nunca se dispare `onClick` → `DatePickerDialog` no se abre. El patrón correcto es `Surface(onClick=…)` (igual que RegistrarGasto/EditarGasto).

## Pasos

- [ ] **Step 1: Imports**

En `ExportarDialog.kt`:
- Quitar: `import androidx.compose.foundation.clickable` (deja de usarse)
- Agregar: `import androidx.compose.foundation.BorderStroke`
- Agregar: `import androidx.compose.foundation.shape.RoundedCornerShape`

- [ ] **Step 2: Reemplazar FechaCampo**

Reemplazar el body de `FechaCampo` (líneas 155-180 actuales) por:

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FechaCampo(
    etiqueta: String,
    valor: LocalDate?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(modifier = modifier) {
        Text(etiqueta, color = AppTheme.TextMuted, fontSize = 12.sp)
        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(4.dp),
            color = AppTheme.CardDark,
            border = BorderStroke(1.dp, AppTheme.BorderColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    valor?.toString() ?: tr("registrar.seleccionarFecha"),
                    color = if (valor == null) AppTheme.TextSubtle else AppTheme.TextPrimary,
                    fontSize = 14.sp
                )
                Icon(
                    AppIcons.Calendario,
                    contentDescription = tr("registrar.seleccionarFecha"),
                    tint = AppTheme.TextMuted
                )
            }
        }
    }
}
```

Asegúrate de que todos los imports utilizados por ese bloque ya existen en el archivo
(`Text`, `Icon`, `Column`, `Row`, `Arrangement`, `Alignment`, `Modifier`, `RoundedCornerShape`,
`BorderStroke`, `Surface`, `AppTheme`, `AppIcons`, `tr`, `LocalDate`, `sp`, `dp`,
`ExperimentalMaterial3Api`, `Composable`, `fillMaxWidth`, `padding`); agrega solo los que falten de la lista del Step 1.

- [ ] **Step 3: Gates**

Run: `.\gradlew.bat :composeApp:jvmTest`
Run: `.\gradlew.bat :composeApp:compileKotlinJvm :composeApp:compileDebugKotlinAndroid`
Expected: tests PASS y BUILD SUCCESSFUL (sin warnings de imports sin usar).

- [ ] **Step 4: Verificación manual (desktop)** — SOLO si el entorno lo permite (entorno GUI). Si no tienes GUI, documentar como pendiente de verificación humana.

Run: `.\gradlew.bat :composeApp:run`
Ajustes → Exportar → alcance "Personalizado" → clic en Desde/Hasta → debe abrirse el `DatePickerDialog`.

## Your Job

1. Lee el archivo actual para ubicar imports + `FechaCampo`.
2. Aplica Steps 1-3. Verifica gates (workdir `D:\GestorGastos`).
3. Step 4 si tienes GUI disponible; si no, no la ejecutes (anótalo en el report).
4. Report.

## When You're in Over Your Head

Siempre está bien parar y reportar BLOCKED/NEEDS_CONTEXT con detalles. No adivines.

## Report Format

Report file: `D:\GestorGastos\.superpowers\sdd\2026-09-11-exportar-hojas-mensuales\task-2-report.md`
Include: qué implementaste, output de ambos gates verbatim, archivos cambiados, si corriste o no la verificación manual y por qué, desviaciones.

Then report back with ONLY (under 15 lines):
- **Status:** DONE | DONE_WITH_CONCERNS | BLOCKED | NEEDS_CONTEXT
- One-line gates summary (tests + compile)
- Your concerns, if any
- The report file path

If BLOCKED or NEEDS_CONTEXT, put the specifics in the final message itself.