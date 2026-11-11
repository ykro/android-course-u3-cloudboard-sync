# ARCHITECTURE_DECISIONS.md (Unidad 3)

Responde cada pregunta con tus palabras, en 3 a 8 líneas, citando archivos o funciones de tu código. Se califica con criterio técnico, no con extensión.

Puntaje (15 puntos, revisión manual): ADR-1 a ADR-6, 10 puntos (ADR-2 incluye tu regla nueva en `firebase/firestore.rules`); tus reglas en la sección 5 de `AGENTS.md` (ADR-7), 2; declaración de uso de IA, 1; y el checklist `SECURITY_AND_QUALITY.md`, 2.

## ADR-1: Fronteras entre UI, ViewModel, Domain y Data

**Pregunta.** Para una acción concreta (por ejemplo, mover una tarjeta ajena), recorre las cuatro capas: qué hace el Composable, qué hace `BoardViewModel`, qué decide el UseCase y qué hace el repositorio. ¿Qué regla de negocio estaría mal ubicada si viviera en el ViewModel y cómo lo notarías en un test?

**Respuesta.**

## ADR-2: Guardias de propiedad en el dominio y en las reglas

**Pregunta.** ¿Por qué tus guardias van en el orden sesión, existencia, propiedad, y qué revelaría el orden inverso? ¿Por qué además necesitas `firebase/firestore.rules` si el UseCase ya rechaza a quien no es dueño? Describe un ataque que las reglas detienen y el UseCase no. Por último, agrega a tus `firestore.rules` **una condición que el codelab no te dio** (por ejemplo, que `updatedAtMs` no retroceda en una edición, que `ownerName` no cambie, o que `updatedAtMs` no esté en el futuro respecto de `request.time`), cópiala aquí y explica qué escritura maliciosa o accidental bloquea y qué escritura legítima podría bloquear por error.

**Respuesta.**

## ADR-3: Qué se reintenta y qué no

**Pregunta.** Justifica qué `RemoteFailure` consideras transitorias. ¿Por qué reintentar un `PERMISSION_DENIED` es un error y no una medida de resiliencia? ¿Para qué sirve el jitter y por qué lo inyectas en lugar de llamar a `Random` dentro de `RetryUtil`?

**Respuesta.**

## ADR-4: Idempotencia y acks perdidos

**Pregunta.** Explica con tu código qué pasa cuando la escritura llega a Firestore pero la respuesta se pierde. ¿Por qué el id se genera antes del reintento y por qué las guardias de borrar no se repiten dentro de él? ¿Qué pasaría si el repositorio usara `collection.add(...)` en lugar de `document(id).set(...)`?

**Respuesta.**

## ADR-5: Sincronización local y nube

**Pregunta.** Firestore guarda en caché local y encola escrituras sin conexión. ¿Qué ve la persona usuaria si escribe sin red y qué parte de tu diseño sigue siendo necesaria aunque el SDK ya reintente? ¿Qué limitación tiene `allow read: if signedIn()` en un tablero real y cómo la resolverías?

**Respuesta.**

## ADR-6: Pruebas sin emulador

**Pregunta.** ¿Qué te permitió probar el ViewModel, los UseCases y `RetryUtil` en la JVM sin Firebase? ¿Qué queda sin probar (por ejemplo `FirestoreBoardRepository` o las reglas) y cómo lo verificarías antes de producción?

**Respuesta.**

## ADR-7: AGENTS.md y uso de IA

**Pregunta.** ¿Qué reglas agregaste a la sección 5 de `AGENTS.md`? Se esperan al menos 3 reglas verificables (que un revisor pueda comprobar en el código o con un test, no consejos genéricos). Describe una respuesta concreta de tu asistente antes y después de agregarlas.

**Respuesta.**

## Declaración de uso de IA

Marca con una `x` y completa.

- [ ] No usé asistentes de IA.
- [ ] Usé asistentes de IA. Herramientas:
- Partes del código o de los tests en las que intervino la IA:
- Errores de la IA que detecté y corregí (al menos uno, con archivo y explicación):
