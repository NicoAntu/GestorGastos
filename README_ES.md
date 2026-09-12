# GestorGastos

Aplicación de gestión de gastos personales mes a mes

## Funcionalidades

- **Registro y edición de gastos**: con descripción, monto, categoría, fecha y opción de pago en cuotas.
- **Cuotas**: generación automática del cronograma, detalle por mes y por cuota, edición y eliminación.
- **Categorías**: gestión completa (crear, editar, eliminar) con color personalizable y detalle por categoría.
- **Resumen mensual**: dashboard con total gastado, total ingresos y balance del mes.
- **Buscador**: búsqueda de gastos por descripción, categoría, mes, etc.
- **Estadísticas**: análisis y visualización del histórico de gastos.
- **Ingreso mensual**: registro del ingreso por mes.
- **Exportar a Excel**: genera un archivo `.xlsx` con el detalle mensual de gastos e ingresos.
- **Importar desde Excel**: lee archivos `.xlsx` (generados por la app o por Excel/LibreOffice) y los carga como gastos e ingresos:
  - Un gasto se identifica por tener un **monto mayor a 0**.
  - Si falta descripción, categoría o fecha, se completan con valores por defecto (`-`, `-`, primer día del mes actual); la categoría `-` se crea automáticamente si no existe.
- **Multidioma**: interfaz en español, inglés y francés.
- **Tema**: interfaz moderna con Material 3 y tema oscuro.

## Tecnologías

- **Kotlin**: 2.3
- **Compose Multiplatform**: 1.10 (Material 3)
- **Koin**: inyección de dependencias
- **Decompose**: navegación y arquitectura
- **SQLDelight**: base de datos SQLite tipada
- **kotlinx-serialization**: serialización JSON
- Generación y lectura de `.xlsx` propia (sin dependencias externas de Excel)

## Requisitos

- JDK 17+
- Android SDK 36 (para compilar la versión Android)

## Compilar y ejecutar

### Aplicación Android (instalación)

La app se distribuye como un instalador APK firmado, disponible en la sección **Releases** del
[repositorio](https://github.com/NicoAntu/GestorGastos/releases).

1. Descargar el archivo APK de la última release.
2. Si el sistema lo pide, permite la instalación desde orígenes desconocidos.
3. Completa el asistente de instalación y abre la app.

### Aplicación Desktop (JVM)

```shell
# Windows
.\gradlew.bat :composeApp:run

# macOS/Linux
./gradlew :composeApp:run
```

### Tests

```shell
.\gradlew.bat :composeApp:jvmTest
```