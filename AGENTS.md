# AGENTS.md: reglas para asistentes de IA en CloudBoard Sync

Este archivo es la base. Como parte de la entrega debes ampliarlo con al menos 3 reglas verificables propias del reto (sección 5) y explicar en `ARCHITECTURE_DECISIONS.md` qué cambió en las respuestas de tu asistente al usarlo.

## 1. Fronteras entre capas (no negociable)

1. `domain/` es Kotlin puro: no importa `com.google.firebase.*`, `android.*` ni `androidx.*`. Depende solo de interfaces (`AuthRepository`, `BoardRepository`, `BoardSummarizer`, `Clock`, `IdGenerator`).
2. Los Composables reciben estado inmutable (`BoardUiState`) y lambdas. No llaman a Firebase, no validan y no deciden permisos.
3. `BoardViewModel` orquesta UseCases y traduce errores a mensajes. No contiene reglas de negocio (títulos, puntos, propiedad).
4. Las implementaciones de Firebase viven en `data/firebase/` y traducen las excepciones del SDK a `RemoteException`.
5. Las dependencias se resuelven con Koin en `di/AppModule.kt`; ninguna clase llama a `getInstance()` fuera de ese archivo.

## 2. Listeners y corrutinas

1. Todo listener de un SDK se convierte en `Flow` con `SnapshotSource.asFlow()` (`callbackFlow` con `awaitClose`). Nunca un `addSnapshotListener` suelto en un Composable o en un `init`.
2. Ningún `catch` se traga una `CancellationException`.
3. En tests: `runTest`, `advanceUntilIdle()` y Turbine. Nada de `Thread.sleep` ni de red.

## 3. Seguridad

1. Ocultar un botón no es seguridad. La propiedad se verifica en los UseCases y en `firebase/firestore.rules` (`resource.data.ownerId == request.auth.uid`).
2. El dueño de una tarjeta sale de la sesión, nunca de un dato que envía la UI.
3. `google-services.json`, `local.properties` y cualquier llave quedan fuera del repositorio.

## 4. Verificación obligatoria antes de proponer un cambio

```bash
./gradlew check
```

No modifiques `DemoTest`, `WorkshopTest`, `ChallengeTest`, `testutil/`, los archivos de Gradle ni `.github/workflows/`.

## 5. Reglas del reto (complétalas tú)

<!-- Escribe aquí al menos 3 reglas verificables (comprobables en el código o con un test)
     que tu asistente debe respetar en UpdateCardUseCase, DeleteCardUseCase y RetryUtil:
     orden de las guardias, qué errores se reintentan, dónde se genera el id, etc. -->
