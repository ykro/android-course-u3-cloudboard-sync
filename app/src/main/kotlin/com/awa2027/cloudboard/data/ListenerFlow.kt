package com.awa2027.cloudboard.data

import kotlinx.coroutines.flow.Flow

/** Lo que devuelve un SDK al registrar un listener (en Firestore, `ListenerRegistration`). */
fun interface ListenerHandle {
    fun remove()
}

/**
 * Cualquier API de callbacks que entrega valores hasta que alguien quita el listener:
 * `addSnapshotListener` de Firestore, `AuthStateListener` de Firebase Auth o un fake en los tests.
 */
fun interface SnapshotSource<T> {
    fun listen(
        onValue: (T) -> Unit,
        onError: (Throwable) -> Unit,
    ): ListenerHandle
}

/**
 * Convierte la fuente en un Flow frío. Cada colector registra su propio listener;
 * cuando el colector se cancela (la pantalla se va), `awaitClose` lo quita.
 * Un error del SDK cierra el Flow con esa excepción.
 */
fun <T> SnapshotSource<T>.asFlow(): Flow<T> {
    // Paso 2 del taller: usa callbackFlow, registra el listener con listen(...),
    // envía cada valor con trySend, cierra con close(error) y quita el listener en awaitClose.
    TODO("Paso 2 del taller: callbackFlow + awaitClose")
}
