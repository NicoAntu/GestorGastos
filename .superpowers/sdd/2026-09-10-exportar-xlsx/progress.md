# SDD ledger — plan: D:\GestorGastos\docs\superpowers\plans\2026-09-10-exportar-xlsx.md

Nota de entorno: D:\GestorGastos NO es un repositorio git. No hay commits.
Gates por tarea: compilación (.\gradlew.bat :composeApp:compileKotlinJvm :composeApp:compileDebugKotlinAndroid) y pruebas (.\gradlew.bat :composeApp:jvmTest). Revisores leen los archivos del brief directamente (no hay diffs git).

## Task 1: ZipWriter (commonMain) + test

Implementer: DONE_WITH_CONCERNS (1/1 passing, output pristine, compileKotlinJvm OK).
Concern del implementer: el helper `aTexto()` del brief estaba roto verbatim — dentro de `buildString {}` el `forEach` liga al receiver StringBuilder vacío, no al ByteArray (shadowing), devolviendo siempre "" (el test nunca podría pasar). Fix mínimo: `this@aTexto.forEach` en XlsxGeneradorTest.kt; ZipWriter intacto. Fix preserva la intención del test (verificar firmas y nombre); no contradice contratos del plan, solo corrige código de test mal escrito en el plan. → Se procede a review; el reviewer juzga el fix.

**Walkthrough del controller (verificación del fix):** correcto. `ByteArray.forEach` y `StringBuilder.forEach` (vía CharSequence) coexisten; `buildString { }` recibe lambda con receiver StringBuilder, y en su cuerpo `forEach` sin calificar resuelve al receiver StringBuilder → itera los chars del (vacío) builder, no los bytes. `this@aTexto.forEach` fuerza iterar el ByteArray exterior. Fix de un token, ningún cambio de comportamiento en ZipWriter.

Task 1: complete (review clean — Approved; 1/1 passing, compileKotlinJvm OK)
Task 1: minor (deferred): test no verifica CRC ni tamaños/offset del central (solo presencia de bytes); hardening sugerido.
Task 1: minor (deferred): `push`/`centralPieza` indirección gratuita (verbatim del plan); sin guardas para nombre>65535 ni data>4GiB (aceptable para xlsx).

Task 2: complete (review clean — Approved; 4/4 tests, gates JVM+Android OK)
Task 2: minor (deferred): `String.format` en porcentajeDe es locale-sensitivo; output en la práctica X,XX% pero sin locale explícito (no determinista).
Task 2: minor (deferred): lista de gastos vacía → no emite filas 2/3, autoFilter A1:E1, G2/I2 ausentes; indefinido pero no crashea (fuera de alcance del brief; ViewModel filtra isEmpty en Task 7).

Task 3: complete (review clean — Approved; gate JVM+Android BUILD SUCCESSFUL)
Task 3: adaptación esperada documentada: nombres de params generados por SQLDelight (`fecha`/`fecha_`, `value_`/`value__`) usados en call sites; .sq e interfaces verbatim. Reutilizó mapper existente de IngresoMensual.
Task 3: minor (deferred): 4 mappers de 13 campos duplicados en GastoRepositoryImpl (incl. preexistentes); extraer un mapper privado paramétrico sería cleanup futuro; NO es nuevo de esta tarea (adds 1 más de los 3 preexistentes).

Task 4: complete (review clean — Approved; gate JVM+Android BUILD SUCCESSFUL)
Task 4: minor (deferred): sin test unitario del rango (MES_ACTUAL fin de mes p.ej. Feb; TODO/FINAL); lógica correcta (plusMonths(1).minusDays(1)) pero no probada. Usa `ejecutar` (mandado por brief) vs estilo `invoke` del proyecto.

Task 5: complete (review clean — Approved; gate compileKotlinJvm OK, Android compila en Task 6)

Task 6: complete (review clean — Approved; gate JVM+Android BUILD SUCCESSFUL)
Task 6: minor (deferred): `registerForActivityResult` como property initializer vs `onCreate`/`lazy`; aceptable pero menos convencional.

Task 7: en review — Approved con 1 Important en conflicto con el plan (pendiente decisión humana) + 2 minors.
Task 7: minor (deferred): éxito no resetea `esError` (queda true de un fallo previo sin mensaje; UI keyed on esError mostraría estilo de error stale) — verbatim del plan.
Task 7: minor (deferred): `guardar()` se basa en bytes/nombreArchivo, no en `listoParaGuardar` — un tap en la ventana entre exportar() y su primera actualización podría guardar bytes stale; negligible porque botón gated, verbatim del plan.
Task 7: Important — `XlsxGenerador.generar(contenido)` en `onSuccess` FUERA del `runCatching` (ExportarViewModel.kt:51): si lanza (p.ej. `LocalDate.parse(fechaIso)` con fecha malformada o fallo en ZipWriter.zip), la excepción escapa del coroutine sin capturar → `exportando=true` nunca se limpia (spinner perpetuo), sin mensaje de error, y excepción no capturada en viewModelScope puede crashear Android. Baja probabilidad (generar es string-building puro sobre datos ya del DB; fechas ISO) pero real. Fix trivial (meter `generar` dentro del mismo runCatching) que DEBÍA según el brief verbatim. → pregunta al humano cuál manda.
Task 7: fix round 1/5 — humano autorizó fix. Implementer original dobló todo el pipeline dentro del runCatching (use case → empty check → contenido → generar → state update): cualquier throw de generación → onFailure limpia exportando + exportar.error + esError. Path sinDatos preservado; minors diferidos NO tocados. Gate BUILD SUCCESSFUL.
Task 7: re-review round 1 — generar-outside-runCatching ADDRESSED; los 3 outcomes de estado verificados correctos; sin breakage nuevo. Out-of-scope minor: runCatching ampliado traga CancellationException en teardown (patrón preexistente, trivial).
Task 7: complete (fix round 1/5, re-review clean — Approved)

Task 8: complete (review clean — Approved; gate BUILD SUCCESSFUL; 11 claves × 3 idiomas verbatim, sin duplicados, ajustes.* intactas)

Task 9: complete (review Approved; gates jvmTest PASS + BUILD SUCCESSFUL; OutlinedTextField(onClick) no existe en M3 1.10.0-alpha05 → fallback Box(clickable) sancionado por el plan; VM fix de una línea aplicado — round 2/5 — re-review verified: guardar() ahora pone exportar.error en fallo → feedback completo en todas las rutas)

FINAL WHOLE-BRANCH REVIEW: halló 1 Critical (doble instancia del VM por factory → snackbar de éxito muerto) + 1 Important (G2/I2/I3 no omitidos con ingreso 0). User decidió "Ambos fixes".
- Fix Critical (snackbar): AjustesScreen pasa viewModel = exportarViewModel al diálogo; limpiarMensaje fuera del dismiss. VERIFIED.
- Fix Important (G/I): XlsxGenerador guarda G2/I2 y I3 con contenido.ingresoTotal > 0; test nuevo generadorConIngresoCeroOmiteBalance (5/5 PASS). VERIFIED.
- Re-review final: READY TO SHIP, sin defects nuevos; solo minors del ledger (incluye 1 nuevo minor opcional: default param koinInject en ExportarDialog unreachable).