# SECURITY_AND_QUALITY.md (Unidad 3)

Marca cada casilla solo si lo verificaste en tu código y escribe en una línea cómo lo comprobaste (test, comando o archivo).

## Capas

- [ ] `domain/` no importa Firebase, Android ni AndroidX. Evidencia:
- [ ] Ningún Composable llama a Firebase, valida datos o decide permisos. Evidencia:
- [ ] Todo listener de un SDK pasa por `asFlow()` y se quita en `awaitClose`. Evidencia:

## Propiedad y reglas

- [ ] Editar y borrar una tarjeta ajena lanzan `NotOwner` sin escribir nada. Evidencia:
- [ ] Sin sesión, editar y borrar lanzan `NotSignedIn` sin consultar el repositorio. Evidencia:
- [ ] El dueño de una tarjeta nueva sale de la sesión. Evidencia:
- [ ] `firestore.rules` exige `ownerId == request.auth.uid` en create, update y delete, e impide cambiar `ownerId`. Evidencia:
- [ ] `firestore.rules` valida campos, título y puntos igual que `CardRules`. Evidencia:

## Resiliencia

- [ ] `RetryUtil` solo reintenta fallos transitorios y nunca la cancelación. Evidencia:
- [ ] La espera nunca supera `maxDelayMs`, ni con jitter ni con muchos reintentos. Evidencia:
- [ ] Un ack perdido al crear, editar o borrar no duplica ni termina en error. Evidencia:

## Secretos y calidad

- [ ] No hay `google-services.json`, `local.properties` ni llaves en el repositorio. Evidencia:
- [ ] `./gradlew check` pasa en local (ktlint, Lint y tests). Evidencia:
