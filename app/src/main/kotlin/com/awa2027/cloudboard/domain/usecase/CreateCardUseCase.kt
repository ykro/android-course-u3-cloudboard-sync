package com.awa2027.cloudboard.domain.usecase

import com.awa2027.cloudboard.domain.model.BoardCard
import com.awa2027.cloudboard.domain.model.CardDraft
import com.awa2027.cloudboard.domain.repository.AuthRepository
import com.awa2027.cloudboard.domain.repository.BoardRepository
import com.awa2027.cloudboard.domain.support.Clock
import com.awa2027.cloudboard.domain.support.IdGenerator
import com.awa2027.cloudboard.domain.sync.RetryPolicy

/**
 * Crea una tarjeta. Orden: sesión, invariantes, id generado UNA vez, escritura.
 * El dueño sale de la sesión, nunca del borrador.
 */
class CreateCardUseCase(
    private val auth: AuthRepository,
    private val board: BoardRepository,
    private val ids: IdGenerator,
    private val clock: Clock,
    // La usarás en el reto (parte C) para que la escritura sobreviva fallos transitorios.
    private val retryPolicy: RetryPolicy,
) {
    suspend operator fun invoke(draft: CardDraft): BoardCard {
        // Paso 3 del taller:
        // 1. auth.requireUser() (lanza NotSignedIn).
        // 2. Título y puntos con CardRules (lanzan InvalidCard) ANTES de escribir.
        // 3. Arma la BoardCard con ids.newId(), el dueño de la sesión y clock.nowMs().
        // 4. board.upsert(card) y devuelve la tarjeta.
        TODO("Paso 3 del taller: CreateCardUseCase")
    }
}
