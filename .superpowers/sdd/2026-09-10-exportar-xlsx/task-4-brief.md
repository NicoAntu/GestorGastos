# Task 4 Brief: ExportarGastosUseCase

Extraído del plan: `D:\GestorGastos\docs\superpowers\plans\2026-09-10-exportar-xlsx.md` (Task 4, líneas 638-706). Este archivo es tu única fuente de requisitos; usa los valores exactos verbatim.

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/angel/gg/domain/usecase/ExportarGastosUseCase.kt`

**Interfaces:**
- Consumes: `GastoRepository.obtenerPorRango(String, String): List<Gasto>`, `IngresoMensualRepository.obtenerEnRango(Long, Long): List<IngresoMensual>`, `ExportarAlcance`, `ExportarDatos` (ambos de `com.angel.gg.domain.model`).
- Produces: `class ExportarGastosUseCase(gastoRepository, ingresoMensualRepository) { suspend fun ejecutar(alcance: ExportarAlcance, desde: LocalDate?, hasta: LocalDate?): ExportarDatos }`

## Step 1: Implementar el use case

`composeApp/src/commonMain/kotlin/com/angel/gg/domain/usecase/ExportarGastosUseCase.kt`:

```kotlin
package com.angel.gg.domain.usecase

import com.angel.gg.domain.model.ExportarAlcance
import com.angel.gg.domain.model.ExportarDatos
import com.angel.gg.domain.repository.GastoRepository
import com.angel.gg.domain.repository.IngresoMensualRepository
import java.time.LocalDate

class ExportarGastosUseCase(
    private val gastoRepository: GastoRepository,
    private val ingresoMensualRepository: IngresoMensualRepository
) {
    suspend fun ejecutar(
        alcance: ExportarAlcance,
        desde: LocalDate?,
        hasta: LocalDate?
    ): ExportarDatos {
        val hoy = LocalDate.now()
        val (fDesde, fHasta) = when (alcance) {
            ExportarAlcance.MES_ACTUAL -> {
                val m = hoy.withDayOfMonth(1)
                Pair(m, m.plusMonths(1).minusDays(1))
            }
            ExportarAlcance.ANIO_ACTUAL ->
                Pair(LocalDate.of(hoy.year, 1, 1), LocalDate.of(hoy.year, 12, 31))
            ExportarAlcance.TODO ->
                Pair(LocalDate.of(1900, 1, 1), LocalDate.of(9999, 12, 31))
            ExportarAlcance.PERSONALIZADO -> {
                val d = requireNotNull(desde) { "Rango personalizado sin fecha desde" }
                val h = requireNotNull(hasta) { "Rango personalizado sin fecha hasta" }
                require(!h.isBefore(d)) { "desde debe ser <= hasta" }
                Pair(d, h)
            }
        }

        val gastos = gastoRepository.obtenerPorRango(fDesde.toString(), fHasta.toString())
        val desdeKey = fDesde.year * 100L + fDesde.monthValue
        val hastaKey = fHasta.year * 100L + fHasta.monthValue
        val ingresoTotal = ingresoMensualRepository.obtenerEnRango(desdeKey, hastaKey)
            .sumOf { it.monto }

        return ExportarDatos(
            gastos = gastos,
            ingresoTotal = ingresoTotal,
            desde = fDesde,
            hasta = fHasta,
            alcance = alcance
        )
    }
}
```

## Step 2: Gate de compilación

Run: `.\gradlew.bat :composeApp:compileKotlinJvm :composeApp:compileDebugKotlinAndroid`
Expected: BUILD SUCCESSFUL.

## Environment / Global Constraints

- Working directory: `D:\GestorGastos`. Work from there (workdir) for all commands.
- IMPORTANT: This project is NOT a git repository. Do NOT attempt git commands or commits.
- Do NOT add any new dependencies.
- `java.time` disponible en commonMain. Verifica que `ExportarDatos`, `ExportarAlcance` existan en `com.angel.gg.domain.model` (creados en Task 2) y que los repos tengan los métodos (Task 3). Si alguna firma real difiere, adáptate y notifícalo.

## Your Job

1. Implementa exactamente el brief. Es transcripción — salvo ajustes de firma reales.
2. Gate de compilación.
3. Self-review.
4. Report.

## When You're in Over Your Head

Siempre está bien parar y decir "esto es demasiado difícil para mí". Si algo no cuadra con el código real, detente y reporta BLOCKED o NEEDS_CONTEXT con detalles. No adivines.

## Before Reporting Back: Self-Review

- Completeness: el use case completo, lógica de rango por alcance correcta, validation PERSONALIZADO (requireNotNull + require), suma de ingreso mensual.
- Quality: sigue el patrón de use cases del proyecto (mira si hay otros en `domain/usecase` y respeta el estilo; sin reestructurar nada fuera de tu archivo).
- Discipline: YAGNI.

## Report Format

Write your full report to: `D:\GestorGastos\.superpowers\sdd\2026-09-10-exportar-xlsx\task-4-report.md`
Include: qué implementaste, output del gate, archivos cambiados, self-review findings, desviaciones.

Then report back with ONLY (under 15 lines):
- **Status:** DONE | DONE_WITH_CONCERNS | BLOCKED | NEEDS_CONTEXT
- One-line gate summary
- Your concerns, if any
- The report file path

If BLOCKED or NEEDS_CONTEXT, put the specifics in the final message itself.