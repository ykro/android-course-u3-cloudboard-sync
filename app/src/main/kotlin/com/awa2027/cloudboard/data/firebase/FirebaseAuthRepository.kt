package com.awa2027.cloudboard.data.firebase

import com.awa2027.cloudboard.data.ListenerHandle
import com.awa2027.cloudboard.data.SnapshotSource
import com.awa2027.cloudboard.data.asFlow
import com.awa2027.cloudboard.domain.model.RemoteException
import com.awa2027.cloudboard.domain.model.RemoteFailure
import com.awa2027.cloudboard.domain.model.SessionUser
import com.awa2027.cloudboard.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.cancellation.CancellationException

/** Implementación con Firebase Auth (inicio de sesión anónimo). Compila; no se ejecuta en los tests. */
class FirebaseAuthRepository(
    private val auth: FirebaseAuth,
) : AuthRepository {
    // El mismo asFlow() del taller: el AuthStateListener se quita cuando el colector se cancela.
    override val session: Flow<SessionUser?> =
        SnapshotSource<SessionUser?> { onValue, _ ->
            val listener = FirebaseAuth.AuthStateListener { onValue(it.currentUser?.toSessionUser()) }
            auth.addAuthStateListener(listener)
            ListenerHandle { auth.removeAuthStateListener(listener) }
        }.asFlow()

    override fun currentUser(): SessionUser? = auth.currentUser?.toSessionUser()

    override suspend fun signIn(): SessionUser =
        try {
            auth
                .signInAnonymously()
                .await()
                .user
                ?.toSessionUser()
                ?: throw RemoteException(RemoteFailure.UNAUTHENTICATED, "Firebase no devolvió un usuario")
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: RemoteException) {
            throw error
        } catch (error: Exception) {
            throw error.toRemoteException()
        }

    override suspend fun signOut() {
        auth.signOut()
    }
}

private fun FirebaseUser.toSessionUser(): SessionUser =
    SessionUser(uid = uid, displayName = displayName?.takeIf { it.isNotBlank() } ?: "Invitado ${uid.take(4)}")
