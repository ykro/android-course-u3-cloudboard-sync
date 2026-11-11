# CloudBoard Sync

Tablero colaborativo en Android con arquitectura en capas, Firebase Auth, Cloud Firestore y sincronización con reintentos.

## Unidad 3

Plantilla de la Unidad 3 del curso.

## Requisitos

- Android Studio estable actual y JDK 21 (el IDE lo incluye).
- SDK de Android en `ANDROID_HOME` o en `local.properties` (no lo subas al repositorio).
- Para ver la app, un emulador o teléfono con Android 8.0 (API 26) o superior. Los tests no necesitan emulador.

## Instalar y correr

Abre la raíz del repositorio en Android Studio y espera la sincronización de Gradle (la primera vez tarda varios minutos). Luego ejecuta la app en un emulador o teléfono, o compila desde la terminal:

```bash
./gradlew assembleDebug
```

Sin `google-services.json` la app arranca en modo demo, con repositorios en memoria. `local.properties` y `google-services.json` no se versionan.

## Tests

```bash
./gradlew testDebugUnitTest      # solo los tests
./gradlew check                  # chequeo completo: ktlint, lint de Android y tests
```

El archivo `AGENTS.md` trae las reglas para el asistente de IA.

El enunciado del reto, la rúbrica y la guía paso a paso están en el curso.
