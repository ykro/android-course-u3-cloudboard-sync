package com.awa2027.cloudboard.data.demo

import com.awa2027.cloudboard.domain.model.BoardCard
import com.awa2027.cloudboard.domain.repository.BoardRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** Repositorio en memoria para el modo demo y los tests. Se comporta como Firestore: escrituras por id. */
class InMemoryBoardRepository(
    initial: List<BoardCard> = emptyList(),
) : BoardRepository {
    private val cards = MutableStateFlow(initial.associateBy { it.id })

    /** Foto actual del almacenamiento (útil en tests). */
    val snapshot: List<BoardCard> get() = cards.value.values.toList()

    override fun observeCards(ownerId: String?): Flow<List<BoardCard>> =
        cards.map { byId ->
            byId.values
                .filter { ownerId == null || it.ownerId == ownerId }
                .sortedByDescending { it.updatedAtMs }
        }

    override suspend fun findCard(cardId: String): BoardCard? = cards.value[cardId]

    override suspend fun upsert(card: BoardCard) {
        cards.update { it + (card.id to card) }
    }

    override suspend fun delete(cardId: String) {
        cards.update { it - cardId }
    }
}
