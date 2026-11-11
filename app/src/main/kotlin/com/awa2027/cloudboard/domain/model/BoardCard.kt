package com.awa2027.cloudboard.domain.model

/** Columnas del tablero en el orden en que se muestran. */
enum class BoardColumn(
    val label: String,
) {
    BACKLOG("Por hacer"),
    IN_PROGRESS("En curso"),
    DONE("Hecho"),
}

/** Tarjeta persistida. `ownerId` es el uid de Firebase Auth de quien la creó y nunca cambia. */
data class BoardCard(
    val id: String,
    val title: String,
    val points: Int,
    val column: BoardColumn,
    val ownerId: String,
    val ownerName: String,
    val updatedAtMs: Long,
)

/** Lo que la UI envía para crear una tarjeta. No trae dueño: el dueño sale de la sesión. */
data class CardDraft(
    val title: String,
    val points: Int,
    val column: BoardColumn = BoardColumn.BACKLOG,
)

/** Cambios parciales sobre una tarjeta existente. `null` significa "no cambiar". */
data class CardChanges(
    val title: String? = null,
    val points: Int? = null,
    val column: BoardColumn? = null,
)

/** Usuario con sesión iniciada. */
data class SessionUser(
    val uid: String,
    val displayName: String,
)
