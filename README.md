# GestorGastos

Aplicación de gestión de gastos personales multiplataforma (Android + Desktop) construida con Kotlin Multiplatform y Compose Multiplatform.

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
- **kotlinx-serialization / kotlinx-datetime**: serialización y fechas
- Generación y lectura de `.xlsx` propia (sin dependencias externas de Excel)

## Estructura del proyecto

```
├── composeApp/
│   └── src/
│       ├── commonMain/     # Código compartido (UI, dominio, datos, exportar/importar)
│       ├── androidMain/    # Implementaciones específicas de Android
│       ├── jvmMain/        # Implementaciones específicas de Desktop (JVM)
│       ├── commonTest/     # Tests unitarios compartidos
│       └── androidMain/    # Recursos Android (manifest, iconos)
├── gradle/
├── build.gradle.kts        # Configuración de la build
└── settings.gradle.kts
```

## Requisitos

- JDK 17+
- Android SDK 36 (para compilar la versión Android)
- Android Studio (recomendado) o IntelliJ IDEA

## Compilar y ejecutar

### Aplicación Android (APK debug)

```shell
# Windows
.\gradlew.bat :composeApp:assembleDebug

# macOS/Linux
./gradlew :composeApp:assembleDebug
```

El APK se genera en `composeApp/build/outputs/apk/debug/composeApp-debug.apk`.

Para instalarlo en un dispositivo conectado:

```shell
adb install -r composeApp/build/outputs/apk/debug/composeApp-debug.apk
```

O copia el APK al teléfono y ábrelo (habilitando la instalación de apps de orígenes desconocidos).

### Aplicación Android (APK release firmado)

El APK de release se firma automáticamente con un keystore local. Crea la configuración de firma una única vez:

1. Genera el keystore:

   ```shell
   keytool -genkey -v -keystore keystore/gestorgastos.jks -alias gestorgastos \
     -keyalg RSA -keysize 2048 -validity 10000
   ```

2. Crea `keystore.properties` en la raíz del proyecto (no se sube a GitHub):

   ```properties
   storeFile=../keystore/gestorgastos.jks
   storePassword=TU_PASSWORD
   keyAlias=gestorgastos
   keyPassword=TU_PASSWORD
   ```

3. Compila:

   ```shell
   .\gradlew.bat :composeApp:assembleRelease
   ```

El APK firmado se genera en `composeApp/build/outputs/apk/release/composeApp-release.apk`.

> [!IMPORTANT]
> Guarda una copia del keystore y de `keystore.properties`: si se pierden no podrás actualizar la app firmada con la misma identidad.

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

## Notas

- La firma de release y los secretos (`keystore/`, `keystore.properties`, `local.properties`) están excluidos del repositorio.
- Si eliminas `local.properties`, indícale a Android Studio la ruta de tu Android SDK.