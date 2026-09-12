# Task 3 Brief: Queries SQLDelight + métodos de repositorio

Extraído del plan: `D:\GestorGastos\docs\superpowers\plans\2026-09-10-exportar-xlsx.md` (Task 3, líneas 528-635). Este archivo es tu única fuente de requisitos; usa los valores exactos verbatim.

**Files:**
- Modify: `composeApp/src/commonMain/sqldelight/com/angel/gg/db/Gasto.sq`
- Modify: `composeApp/src/commonMain/sqldelight/com/angel/gg/db/IngresoMensual.sq`
- Modify: `composeApp/src/commonMain/kotlin/com/angel/gg/domain/repository/GastoRepository.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/angel/gg/data/repository/GastoRepositoryImpl.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/angel/gg/domain/repository/IngresoMensualRepository.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/angel/gg/data/repository/IngresoMensualRepositoryImpl.kt`

**Interfaces:**
- Producer (useCase, Task 4): `GastoRepository.obtenerPorRango(desde: String, hasta: String): List<Gasto>` y `IngresoMensualRepository.obtenerEnRango(desdeKey: Long, hastaKey: Long): List<IngresoMensual>` (ambos `suspend`).

## Paso previo obligatorio: leer los archivos existentes

Antes de tocar nada, LEE los 6 archivos listados arriba. Necesitas ver:
- El contenido actual de `Gasto.sq` (sobre todo la query `obtenerPorMes` y su mapper generado — la nueva query es calcada).
- El contenido actual de `IngresoMensual.sq`.
- Las interfaces `GastoRepository` e `IngresoMensualRepository` actuales.
- La implementación `GastoRepositoryImpl` (verás el patrón con `withContext(Dispatchers.Default)` y los mappers) e `IngresoMensualRepositoryImpl`.

## Step 1: Agregar la query `obtenerPorRango` en Gasto.sq

Añadir al final de `Gasto.sq`:

```sql
-- Gastos en un rango de fechas (para exportación)
obtenerPorRango:
SELECT
    g.*,
    c.nombre      AS categoria_nombre,
    c.color       AS categoria_color,
    c.icono       AS categoria_icono
FROM Gasto g
INNER JOIN Categoria c ON g.categoria_id = c.id
WHERE g.fecha >= ? AND g.fecha <= ?
ORDER BY g.fecha ASC, g.rowid ASC;
```

## Step 2: Agregar la query `obtenerEnRango` en IngresoMensual.sq

Añadir al final de `IngresoMensual.sq`:

```sql
-- Ingresos por rango de meses (para exportación)
obtenerEnRango:
SELECT * FROM IngresoMensual
WHERE (anio * 100 + mes) >= ? AND (anio * 100 + mes) <= ?;
```

## Step 3: Ampliar la interfaz GastoRepository

En `GastoRepository.kt`, agregar:

```kotlin
    // Rango de fechas (exportación)
    suspend fun obtenerPorRango(desde: String, hasta: String): List<Gasto>
```

## Step 4: Implementar en GastoRepositoryImpl

Agregar el override y el mapper (copia del patrón del mapper `ObtenerPorMes`):

```kotlin
    override suspend fun obtenerPorRango(desde: String, hasta: String): List<Gasto> =
        withContext(Dispatchers.Default) {
            queries.obtenerPorRango(desde = desde, hasta = hasta)
                .executeAsList()
                .map { it.toDomain() }
        }
```

```kotlin
    private fun com.angel.gg.db.ObtenerPorRango.toDomain() = Gasto(
        id              = id,
        monto           = monto,
        descripcion     = descripcion,
        categoriaId     = categoria_id,
        categoriaNombre = categoria_nombre,
        categoriaColor  = categoria_color,
        categoriaIcono  = categoria_icono,
        fecha           = fecha,
        anio            = anio.toInt(),
        mes             = mes.toInt(),
        cuotaActual     = cuota_actual?.toInt(),
        cuotaTotal      = cuota_total?.toInt(),
        idPadre         = id_padre
    )
```

Nota: si el mapper de `ObtenerPorMes` ya existente en el archivo tiene exactamente esta forma, puedes extraer un mapper reutilizable; pero lo más sencillo y seguro es copiar el patrón existente. No cambies el comportamiento del resto del archivo.

## Step 5: Ampliar la interfaz IngresoMensualRepository

En `IngresoMensualRepository.kt`:

```kotlin
    suspend fun obtenerEnRango(desdeKey: Long, hastaKey: Long): List<IngresoMensual>
```

## Step 6: Implementar en IngresoMensualRepositoryImpl

```kotlin
    override suspend fun obtenerEnRango(desdeKey: Long, hastaKey: Long): List<IngresoMensual> =
        withContext(Dispatchers.Default) {
            queries.obtenerEnRango(desdeKey = desdeKey, hastaKey = hastaKey)
                .executeAsList()
                .map { com.angel.gg.db.IngresoMensual(it.id, it.anio.toInt(), it.mes.toInt(), it.monto) }
        }
```

Verificar el nombre de la clase generada por SQLDelight para la tabla IngresoMensual (mira cómo la mapea el Repo existente; típicamente también se llama `IngresoMensual`; si difiere, ajustar).

El tipo `IngresoMensual` del dominio está en `com.angel.gg.domain.model` y del generado en `com.angel.gg.db` — mira cómo los importa el archivo actual y mantén el mismo estilo.

## Step 7: Gate de compilación

Run: `.\gradlew.bat :composeApp:compileKotlinJvm :composeApp:compileDebugKotlinAndroid`
Expected: BUILD SUCCESSFUL. (Si el mapper generado no coincide, revisar el paquete `com.angel.gg.db` generado.)

## Environment / Global Constraints

- Working directory: `D:\GestorGastos`. Work from there (workdir) for all commands.
- IMPORTANT: This project is NOT a git repository. No git commands or commits.
- Do NOT add any new dependencies.

## General instructions

- Sigue TDD cuando aplique; aquí el gate principal es la compilación (las queries generadas se compilan por SQLDelight). No hay tests unitarios nuevos para esta tarea.
- Si encuentras algo distinto a lo esperado en los archivos existentes (nombres de query, formato de mappers), adáptate al estilo real del proyecto y nota la diferencia en tu report.
- No reestructures nada fuera de tu tarea.

## Before Reporting Back: Self-Review

- Completeness: las 6 queries/métodos pedidos están? los mappers devuelven todos los campos en orden correcto?
- Quality: sigue los patrones del proyecto (dónde se ponen los mappers, imports, estilo de `withContext`)?
- Discipline: YAGNI, solo lo pedido.

## Report Format

Write your full report to: `D:\GestorGastos\.superpowers\sdd\2026-09-10-exportar-xlsx\task-3-report.md`
Include: qué implementaste, output del gate de compilación, archivos cambiados, self-review findings, cualquier desviación del brief.

Then report back with ONLY (under 15 lines):
- **Status:** DONE | DONE_WITH_CONCERNS | BLOCKED | NEEDS_CONTEXT
- One-line summary of the gate (e.g. "compile JVM+Android BUILD SUCCESSFUL")
- Your concerns, if any
- The report file path

If BLOCKED or NEEDS_CONTEXT, put the specifics in the final message itself.