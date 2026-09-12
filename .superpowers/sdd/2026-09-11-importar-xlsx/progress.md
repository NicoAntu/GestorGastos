# SDD ledger - plan: docs/superpowers/plans/2026-09-11-importar-xlsx.md

Task 1: implementer DONE_WITH_CONCERNS - fix plan: 'importar xlsx' leer necsita OK(ResultadoImportacion(...)) para compilar (C : R de getOrElse); plan corregido; nota 1: SinGastos out-of-scope en Task 1
Task 1: complete (review clean; 0 BLOCK, 0 FLAG, 2 INFO; deviation Ok(...) confirmed-correct)

Task 2: minor (deferred): unused import assertTrue en ImportarGastosUseCaseTest (viene del brief, sin warning),Task 2: complete (review clean; 0 BLOCK 0 FLAG 2 INFO; deviations suspend-resolver + runBlocking-tests confirmed-correct)

Task 3: minor (deferred): unused import java.io.File en ImportarLectura.jvm.kt (del brief, warning solo),Task 3: complete (review clean; 0 BLOCK 0 FLAG 4 INFO)

Task 4: minor (deferred): unused import setValue en ImportarDialog.kt (cosmetico; sin warning; verificación manual GUI delegada al humano),Task 4: complete (review clean; 0 BLOCK 0 FLAG 2 INFO)

Final review: READY TO SHIP (0 BLOCK; 1 FLAG F1 - teorico, no bloquea - parked: fallo de crear() a mitad de lote imposible con archivos exportados por la app; 6 INFO parked, la mayoria por-diseno o esquinas teoricas; I2 FALSE POSITIVE - ImportarLectura.jvm.kt no tiene import java.io.File (linea 3 es javax.swing.JFileChooser); triage deferred minors: assertTrue y setValue = acceptable)
Feature: complete - 18/18 tests PASS (fresh: ImportarXlsxTest 5 + ImportarGastosUseCaseTest 5 + XlsxGeneradorTest 8), compileKotlinJvm + compileDebugKotlinAndroid BUILD SUCCESSFUL. Pendiente humano: verificacion manual GUI (desktop + Android). Plan 2026-09-11-importar-xlsx.md es el registro.
FIX POST-ENTREGA (manual verify): Importar xlsx reguardado por Excel devolvia el mensaje falso de no contiene gastos. Root cause: ZipReader solo leia STORED y no inflaba DEFLATE (metodo 8). Fix: expect fun inflarDeflate (jvm/android, Inflater(true)) + ZipReader lee el metodo; guard data-descriptor. Regression: DeflateXlsxTest con base64 del archivo real (Ok, 3 filas + 1 ingreso). Gates: jvmTest 19/19, compiles SUCCESSFUL. Pendiente humano: re-testeo manual del xlsx reguardado por Excel + checklist 1-6.
MEJORA por requerimiento: fila = gasto nuevo solo si monto > 0; si falta descripcion/categoria/fecha usan defaults ("-"/"-"/1 del mes actual). Implementado en ImportarXlsx.hojaDe; 7 tests nuevos en ImportarXlsxTest. Gates: jvmTest 26/26, compiles SUCCESSFUL. Pendiente humano: retesteo manual (gastos sin campos, categoria "-" auto-creada, fecha 1 mes actual).
