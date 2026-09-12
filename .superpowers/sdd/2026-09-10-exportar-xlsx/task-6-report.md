# Task 6 Report: actual Android (SAF) + registro en MainActivity + DI

Fecha: 2026-09-10

## Qué implementé

1. **Creado** `composeApp/src/androidMain/kotlin/com/angel/gg/export/ExportarGuardado.android.kt`:
   - `object ExportarPuente`: puente entre el launcher de SAF (registrado en `MainActivity`) y el suspend `guardar(...)`. Guarda el launcher y el contexto, mantiene un `CompletableDeferred<Uri?>` para la resolución asíncrona del URI, y escribe los bytes en el `openOutputStream` (en `Dispatchers.IO`).
   - `actual class ExportarGuardado`: delega en `ExportarPuente.guardar(...)`.

2. **Modificado** `MainActivity.kt`: registro del launcher `ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")` (campo privado de clase) + `ExportarPuente.registrar({ crearDocumento.launch(it) }, applicationContext)` en `onCreate`, justo antes de crear el `RootComponent`. El resto del `onCreate` (creación de `RootComponent` y `setContent { App(...) }`) quedó intacto; `initKoin`/`androidModule` sin tocar.

3. **Modificado** `di/AndroidModule.kt`: añadido `single<ExportarGuardado> { ExportarGuardado() }` con su import.

4. **Modificado** `di/DesktopModule.kt`: añadido `single<ExportarGuardado> { ExportarGuardado() }` con su import.

## Gate de compilación

**Comando:** `.\gradlew.bat :composeApp:compileKotlinJvm :composeApp:compileDebugKotlinAndroid`

**Resultado:** `BUILD SUCCESSFUL in 6s` (25 actionable tasks: 3 executed, 22 up-to-date).

## Archivos cambiados

- `composeApp/src/androidMain/kotlin/com/angel/gg/export/ExportarGuardado.android.kt` (nuevo)
- `composeApp/src/androidMain/kotlin/com/angel/gg/MainActivity.kt`
- `composeApp/src/androidMain/kotlin/com/angel/gg/di/AndroidModule.kt`
- `composeApp/src/jvmMain/kotlin/com/angel/gg/di/DesktopModule.kt`

## Self-review

- **Completeness:** 4 archivos correctos (1 nuevo + 3 modificados); beans `single<ExportarGuardado>` presentes en BOTH `androidModule` y `desktopModule`. El `expect` de Task 5 (commonMain) tiene ahora ambos actuals (JVM de Task 5, Android de Task 6).
- **Calidad / thread-safety del puente:**
  - `CompletableDeferred.await()` suspende el coroutine del VM (no bloquea el main thread); `resolver(uri)` se invoca desde el callback del activity result (main thread) — `complete` es thread-safe.
  - `openOutputStream(...)` se ejecuta dentro de `withContext(Dispatchers.IO)`.
  - `launcher == null` → retorna `false` temprano (sin lanzar). Cancelación del SAF por el usuario → `resolver(null)` completa con `null` → `await() ?: return false`. Correcto.
  - Edge: si se llamara `guardar(...)` dos veces concurrentes, el `pendiente` se sobrescribiría — aceptable aquí (un solo flujo de exportación iniciado por el usuario; YAGNI).
  - `registrar(...)` se llama en cada `onCreate` (recreación de Activity): regenera la referencia al launcher; correcto.
- **YAGNI:** sin código adicional más allá de lo que pide el brief; todo verbatim.

## Desviaciones

Ninguna. Los tres archivos destino coincidían exactamente con el estado descrito en el brief (MainActivity con `onCreate` simple, ambos modules con `DatabaseDriverFactory`/`SettingsStore`). No se agregaron dependencias. Sin git (proyecto no es repo).