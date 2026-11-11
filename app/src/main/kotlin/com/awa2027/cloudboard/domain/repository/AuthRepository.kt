package com.awa2027.cloudboard.domain.repository

import com.awa2027.cloudboard.domain.model.SessionUser
import kotlinx.coroutines.flow.Flow

/** Frontera con Firebase Auth. El dominio solo ve esta interfaz. */
interface AuthRepository {
    /** Sesión actual y sus cambios. Emite `null` cuando no hay sesión. */
    val session: Flow<SessionUser?>

    /** Usuario con sesión en este instante, o `null`. */
    fun currentUser(): SessionUser?

    /** Inicia sesión (anónima en Firebase; en el modo demo alterna entre usuarios de prueba). */
    suspend fun signIn(): SessionUser

    suspend fun signOut()
}
