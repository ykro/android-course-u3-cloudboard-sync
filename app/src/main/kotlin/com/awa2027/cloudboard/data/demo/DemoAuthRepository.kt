package com.awa2027.cloudboard.data.demo

import com.awa2027.cloudboard.domain.model.SessionUser
import com.awa2027.cloudboard.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Sesión del modo demo. `signIn` alterna entre los usuarios de prueba para ver las guardias en acción. */
class DemoAuthRepository(
    private val users: List<SessionUser>,
    initial: SessionUser? = users.firstOrNull(),
) : AuthRepository {
    private val current = MutableStateFlow(initial)

    override val session: StateFlow<SessionUser?> = current

    override fun currentUser(): SessionUser? = current.value

    override suspend fun signIn(): SessionUser {
        val index = users.indexOf(current.value)
        val next = users[(index + 1).mod(users.size)]
        current.value = next
        return next
    }

    override suspend fun signOut() {
        current.value = null
    }
}
