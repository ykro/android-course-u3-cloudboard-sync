package com.awa2027.cloudboard.domain.repository

import com.awa2027.cloudboard.domain.model.BoardCard
import kotlinx.coroutines.flow.Flow

/**
 * Frontera con Cloud Firestore. Las escrituras son idempotentes por id:
 * repetir `upsert` con la misma tarjeta o `delete` con el mismo id deja el mismo estado.
 */
interface BoardRepository {
    /** Tarjetas del tablero en tiempo real. Con `ownerId` solo las de ese dueño (requiere índice compuesto). */
    fun observeCards(ownerId: String? = null): Flow<List<BoardCard>>

    suspend fun findCard(cardId: String): BoardCard?

    /** Crea o reemplaza la tarjeta con ese id (equivale a `document(id).set(...)`). */
    suspend fun upsert(card: BoardCard)

    /** Borra la tarjeta; borrar un id que ya no existe no es un error. */
    suspend fun delete(cardId: String)
}
