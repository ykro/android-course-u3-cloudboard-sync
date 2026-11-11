package com.awa2027.cloudboard.domain.usecase

import com.awa2027.cloudboard.domain.model.BoardCard
import com.awa2027.cloudboard.domain.model.CardChanges
import com.awa2027.cloudboard.domain.repository.AuthRepository
import com.awa2027.cloudboard.domain.repository.BoardRepository
import com.awa2027.cloudboard.domain.support.Clock
import com.awa2027.cloudboard.domain.sync.RetryPolicy

/** Edita o mueve una tarjeta propia. El dueño y el id nunca cambian. */
class UpdateCardUseCase(
    private val auth: AuthRepository,
    private val board: BoardRepository,
    private val clock: Clock,
    private val retryPolicy: RetryPolicy,
) {
    suspend operator fun invoke(
        cardId: String,
        changes: CardChanges,
    ): BoardCard = TODO("Reto, partes A y C: UpdateCardUseCase")
}
