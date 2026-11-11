package com.awa2027.cloudboard.domain.usecase

import com.awa2027.cloudboard.domain.model.BoardCard
import com.awa2027.cloudboard.domain.repository.BoardRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Tablero en tiempo real, ordenado por columna y, dentro de cada columna, de lo más reciente a lo más antiguo. */
class ObserveBoardUseCase(
    private val board: BoardRepository,
) {
    operator fun invoke(ownerId: String? = null): Flow<List<BoardCard>> =
        board.observeCards(ownerId).map { cards ->
            cards.sortedWith(
                compareBy<BoardCard> { it.column.ordinal }
                    .thenByDescending { it.updatedAtMs }
                    .thenBy { it.id },
            )
        }
}
