# Task 8 Brief: Claves i18n `exportar.*` (ES/EN/FR)

Extraído del plan: `D:\GestorGastos\docs\superpowers\plans\2026-09-10-exportar-xlsx.md` (Task 8, líneas 1040-1100). Este archivo es tu única fuente de requisitos; usa los valores exactos verbatim.

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/angel/gg/presentation/i18n/EsStrings.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/angel/gg/presentation/i18n/EnStrings.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/angel/gg/presentation/i18n/FrStrings.kt`

## Paso previo: leer la estructura

Los 3 archivos tienen la misma forma: `internal object EsStrings/EnStrings/FrStrings { val map = mapOf( ... ) }` donde el map termina con la sección `// Ajustes` (línea ~150) que cierra con `"ajustes.ir" to "..."` seguido de `)`. Después del map vienen `val meses = listOf(...)` y el cierre de la clase.

Las entradas se insertan justo DESPUÉS de `"ajustes.ir" to "..."` y ANTES del cierre `)`. ADD no reemplaza nada de lo existente. Observa que las claves `ajustes.exportar` ya existen ("Exportar como xls" etc.) — NO las toques; tu sección es nueva con prefijo `exportar.`.

Mira cómo se alinean las columnas de `to` (padding) y usa el mismo estilo.

## Step 1: Agregar las claves (EsStrings)

Justo después de `"ajustes.ir" to "Ir"` y antes de `)`:

```kotlin
        // Exportar
        "exportar.titulo"       to "Exportar a Excel",
        "exportar.mesActual"    to "Mes actual",
        "exportar.anioActual"   to "Año actual",
        "exportar.todo"         to "Todo",
        "exportar.personalizado" to "Personalizado",
        "exportar.desde"        to "Desde",
        "exportar.hasta"        to "Hasta",
        "exportar.boton"        to "Exportar",
        "exportar.exito"        to "Exportación completada",
        "exportar.error"        to "No se pudo exportar",
        "exportar.sinDatos"     to "No hay gastos en este período"
```

Nota: `"exportar.titulo"` y el resto — la alineación del `to` puede requerir ajuste al estilo del archivo (el map usa columnas alineadas; los implementers suelen alinear todo el bloque). Mantén la alineación coherente con las claves vecinas (columna del `to`).

## Step 2: Agregar las claves (EnStrings)

```kotlin
        // Export
        "exportar.titulo"       to "Export to Excel",
        "exportar.mesActual"    to "Current month",
        "exportar.anioActual"   to "Current year",
        "exportar.todo"         to "All",
        "exportar.personalizado" to "Custom",
        "exportar.desde"        to "From",
        "exportar.hasta"        to "To",
        "exportar.boton"        to "Export",
        "exportar.exito"        to "Export completed",
        "exportar.error"        to "Export failed",
        "exportar.sinDatos"     to "No expenses in this period"
```

## Step 3: Agregar las claves (FrStrings)

```kotlin
        // Exportation
        "exportar.titulo"       to "Exporter vers Excel",
        "exportar.mesActual"    to "Mois actuel",
        "exportar.anioActual"   to "Année actuelle",
        "exportar.todo"         to "Tout",
        "exportar.personalizado" to "Personnalisée",
        "exportar.desde"        to "Depuis",
        "exportar.hasta"        to "Jusqu'à",
        "exportar.boton"        to "Exporter",
        "exportar.exito"        to "Exportation terminée",
        "exportar.error"        to "Échec de l'exportation",
        "exportar.sinDatos"     to "Aucune dépense sur cette période"
```

## Step 4: Gate de compilación

Run: `.\gradlew.bat :composeApp:compileKotlinJvm :composeApp:compileDebugKotlinAndroid`
Expected: BUILD SUCCESSFUL.

## Environment / Global Constraints

- Working directory: `D:\GestorGastos`. Work from there (workdir) para todos los comandos.
- IMPORTANT: Este proyecto NO es un repositorio git. No uses git ni commits.
- No agregar dependencias nuevas.

## Your Job

1. Implementa Steps 1-3 exactamente.
2. Gate (Step 4).
3. Self-review: completeness (11 claves × 3 idiomas, mismas llaves), calidad (alineación del map, sin duplicar claves), YAGNI.
4. Report.

## When You're in Over Your Head

Siempre está bien parar y reportar BLOCKED/NEEDS_CONTEXT con detalles. No adivines.

## Report Format

Report file: `D:\GestorGastos\.superpowers\sdd\2026-09-10-exportar-xlsx\task-8-report.md`
Include: qué implementaste, output del gate, archivos cambiados, self-review findings, desviaciones.

Then report back with ONLY (under 15 lines):
- **Status:** DONE | DONE_WITH_CONCERNS | BLOCKED | NEEDS_CONTEXT
- One-line gate summary
- Your concerns, if any
- The report file path

If BLOCKED or NEEDS_CONTEXT, put the specifics in the final message itself.