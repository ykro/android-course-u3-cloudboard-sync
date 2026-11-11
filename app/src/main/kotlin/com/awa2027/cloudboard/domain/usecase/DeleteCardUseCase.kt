package com.awa2027.cloudboard.domain.usecase

import com.awa2027.cloudboard.domain.repository.AuthRepository
import com.awa2027.cloudboard.domain.repository.BoardRepository
import com.awa2027.cloudboard.domain.sync.RetryPolicy

/** Borra una tarjeta propia. */
class DeleteCardUseCase(
    private val auth: AuthRepository,
    private val board: BoardRepository,
    private val retryPolicy: RetryPolicy,
) {
    suspend operator fun invoke(cardId: String): Unit = TODO("Reto, partes A y C: DeleteCardUseCase")
}
