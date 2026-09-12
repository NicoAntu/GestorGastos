# Task 1 Report: ZipWriter (commonMain) + test

## What I implemented

Pure-Kotlin ZIP writer with byte-level manual layout, stored verbatim from the brief:

- `composeApp/src/commonMain/kotlin/com/angel/gg/export/ZipWriter.kt` — `object ZipWriter` with:
  - `zip(entries: List<Pair<String, ByteArray>>): ByteArray` producing a minimal ZIP (local file headers + STORED payloads + central directory + EOCD), UTF-8 filename flag (0x0800), version needed 20, date 1980-01-01, own CRC-32 (table-driven).
  - `crc32(data: ByteArray): Long` used for entry CRCs.
  - `internal class BuilderPiezas` — little-endian u16/u32/bytes accumulator.
- `composeApp/src/commonTest/kotlin/com/angel/gg/export/XlsxGeneradorTest.kt` — the test from the brief, with one minimal fix (see below).

No new dependencies added. Nothing outside the two files was touched.

## What I tested and test results

- `.\gradlew.bat :composeApp:jvmTest` → **BUILD SUCCESSFUL**, 1 test, 0 failures, 0 errors, pristine output (no warnings).
  - `zipGeneradoTieneFirmasYNombre[jvm]` PASS: asserts local-file-header sig `PK\x03\x04` at start, central-directory sig `PK\x01\x02` present, EOCD sig `PK\x05\x06` present, entry name `hola.txt` present, content `mundo` present.
- `.\gradlew.bat :composeApp:compileKotlinJvm` → **BUILD SUCCESSFUL**.

The produced archive was also sanity-inspected during debugging: first bytes `0x50 0x4b 0x03 0x04`, version 20, flag 0x0800, method 0 (STORED), and CRC `0x65b4200f` for "mundo" — consistent with a valid STORED ZIP layout (will be validated as a real xlsx/zip by later tasks).

## TDD Evidence

- **RED** — `.\gradlew.bat :composeApp:jvmTest` (test written, no ZipWriter yet):
  ```
  > Task :composeApp:compileTestKotlinJvm FAILED
  file:///D:/GestorGastos/composeApp/src/commonTest/kotlin/com/angel/gg/export/XlsxGeneradorTest.kt:13:21 Unresolved
  reference 'ZipWriter'.
  ...
  BUILD FAILED in 10s
  ```
  Expected: ZipWriter doesn't exist — exact reason the RED step must fail.

- **GREEN** — `.\gradlew.bat :composeApp:jvmTest` after implementing ZipWriter:
  ```
  > Task :composeApp:jvmTest
  BUILD SUCCESSFUL
  1 test completed, 0 failed
  ```
  Test XML: `tests="1" skipped="0" failures="0" errors="0"`.

- **Gate** — `.\gradlew.bat :composeApp:compileKotlinJvm` → BUILD SUCCESSFUL.

## Files changed

- Created: `composeApp/src/commonMain/kotlin/com/angel/gg/export/ZipWriter.kt`
- Created: `composeApp/src/commonTest/kotlin/com/angel/gg/export/XlsxGeneradorTest.kt`

## Self-review findings

- **Completeness**: All brief steps executed (test-first, RED, implementation, GREEN, compile gate). Implementation and BuilderPiezas copied verbatim.
- **Quality/Discipline**: No restructuring, no extra dependencies, no overbuilding. Debug scratch test created during diagnosis was deleted before finishing.
- **One necessary deviation from verbatim (genuine defect in the brief's test helper)**:
  The brief's `aTexto()` reads:
  ```kotlin
  buildString { forEach { append((it.toInt() and 0xFF).toChar()) } }
  ```
  Inside `buildString {}` the implicit receiver is the `StringBuilder` (a `CharSequence`). `forEach` therefore binds to the *StringBuilder's empty char sequence*, not the `ByteArray`. The compiler even warned `'fun toInt(): Int' is deprecated. Conversion of Char to Number is deprecated` — proof `it` is a `Char`, not a `Byte`. Result: `aTexto()` always returned `""`, so the test failed at the very first assert (`"".startsWith("PK\u0003\u0004")`) and could never pass as written. I confirmed this empirically (debug test printed `aTexto.len=0` while the raw bytes were correct: `0x50,0x4b,0x03,0x04,...` — so ZipWriter itself was fine).
  **Minimal fix** (one token): `buildString { this@aTexto.forEach { append((it.toInt() and 0xFF).toChar()) } }` — forces iteration of the ByteArray as the brief clearly intended (byte → `(it.toInt() and 0xFF)` → `Char`). All 5 asserts now pass and the deprecation warning is gone (output pristine). This does not change the assertions, the data, or ZipWriter in any way.

## Issues / concerns

1. **Brief test defect** (described above) — fixed with a one-token change and flagged here. If strict verbatim fidelity is mandatory, this test can never pass; a maintainer should confirm the fix is acceptable.
2. The REST of the brief (ZipWriter.kt) was transcribed with zero changes and produces valid STORED ZIP structure; a real round-trip validation against a ZIP reader is explicitly deferred to Task 3 per the brief.