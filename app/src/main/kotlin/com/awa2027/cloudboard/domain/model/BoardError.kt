package com.awa2027.cloudboard.domain.model

/**
 * Errores de dominio tipados. Los lanzan los UseCases antes de tocar el repositorio;
 * la UI solo los traduce a mensajes, nunca decide permisos.
 */
sealed class BoardError(
    message: String,
) : Exception(message) {
    class NotSignedIn : BoardError("No hay una sesión activa")

    class CardNotFound(
        val cardId: String,
    ) : BoardError("No existe la tarjeta $cardId")

    class NotOwner(
        val cardId: String,
        val actorId: String,
    ) : BoardError("La tarjeta $cardId no pertenece a $actorId")

    class InvalidCard(
        val field: Field,
        reason: String,
    ) : BoardError(reason)

    enum class Field { TITLE, POINTS }
}

/**
 * Categorías de fallo remoto, alineadas con los códigos de Firestore y gRPC.
 * `transient = true` significa que repetir la misma operación más tarde puede funcionar.
 */
enum class RemoteFailure(
    val transient: Boolean,
) {
    UNAVAILABLE(true),
    DEADLINE_EXCEEDED(true),
    RESOURCE_EXHAUSTED(true),
    ABORTED(true),
    PERMISSION_DENIED(false),
    UNAUTHENTICATED(false),
    NOT_FOUND(false),
    INVALID_ARGUMENT(false),
    UNKNOWN(false),
}

/** Fallo de la capa de datos ya traducido: el dominio no conoce las excepciones del SDK. */
class RemoteException(
    val failure: RemoteFailure,
    message: String = failure.name,
    cause: Throwable? = null,
) : Exception(message, cause)
