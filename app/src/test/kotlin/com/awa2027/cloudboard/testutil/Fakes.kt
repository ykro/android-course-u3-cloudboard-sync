package com.awa2027.cloudboard.testutil

import com.awa2027.cloudboard.data.ListenerHandle
import com.awa2027.cloudboard.data.SnapshotSource
import com.awa2027.cloudboard.data.demo.InMemoryBoardRepository
import com.awa2027.cloudboard.domain.model.BoardCard
import com.awa2027.cloudboard.domain.model.BoardColumn
import com.awa2027.cloudboard.domain.model.RemoteException
import com.awa2027.cloudboard.domain.model.RemoteFailure
import com.awa2027.cloudboard.domain.model.SessionUser
import com.awa2027.cloudboard.domain.repository.AuthRepository
import com.awa2027.cloudboard.domain.repository.BoardRepository
import com.awa2027.cloudboard.domain.support.Clock
import com.awa2027.cloudboard.domain.support.IdGenerator
import com.awa2027.cloudboard.domain.sync.RetryPolicy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

val ANA = SessionUser(uid = "uid-ana", displayName = "Ana")
val LUIS = SessionUser(uid = "uid-luis", displayName = "Luis")

/** Política de reintentos de los tests: sin jitter, para que las esperas sean exactas. */
val TEST_RETRY = RetryPolicy(maxAttempts = 3, initialDelayMs = 100, maxDelayMs = 1_000)

fun card(
    id: String,
    title: String = "Tarjeta $id",
    points: Int = 3,
    column: BoardColumn = BoardColumn.BACKLOG,
    owner: SessionUser = ANA,
    updatedAtMs: Long = 1_000,
) = BoardCard(id, title, points, column, owner.uid, owner.displayName, updatedAtMs)

class FakeAuthRepository(
    initial: SessionUser?,
) : AuthRepository {
    private val state = MutableStateFlow(initial)
    override val session: StateFlow<SessionUser?> = state

    override fun currentUser(): SessionUser? = state.value

    override suspend fun signIn(): SessionUser = state.value ?: ANA.also { state.value = it }

    override suspend fun signOut() {
        state.value = null
    }

    fun switchTo(user: SessionUser?) {
        state.value = user
    }
}

class FixedClock(
    var now: Long = 50_000,
) : Clock {
    override fun nowMs(): Long = now
}

class SequentialIds(
    private val prefix: String = "id",
) : IdGenerator {
    var calls = 0
        private set

    override fun newId(): String = "$prefix-${++calls}"
}

/**
 * Repositorio con fallos programados. Registra cada llamada ("find:x", "upsert:x", "delete:x").
 * `afterCommit = true` simula un ack perdido: la escritura se aplica y aun así la llamada falla.
 */
class ScriptedBoardRepository(
    initial: List<BoardCard> = emptyList(),
) : BoardRepository {
    private val store = InMemoryBoardRepository(initial)
    private val planned = mutableListOf<Planned>()
    val calls = mutableListOf<String>()

    val cards: List<BoardCard> get() = store.snapshot
    val writes: Int get() = calls.count { it.startsWith("upsert:") || it.startsWith("delete:") }

    fun failNext(
        operation: String,
        failure: RemoteFailure,
        times: Int = 1,
        afterCommit: Boolean = false,
    ) {
        repeat(times) { planned += Planned(operation, failure, afterCommit) }
    }

    override fun observeCards(ownerId: String?): Flow<List<BoardCard>> = store.observeCards(ownerId)

    override suspend fun findCard(cardId: String): BoardCard? {
        calls += "find:$cardId"
        failOrRun("find") { }
        return store.findCard(cardId)
    }

    override suspend fun upsert(card: BoardCard) {
        calls += "upsert:${card.id}"
        failOrRun("upsert") { store.upsert(card) }
    }

    override suspend fun delete(cardId: String) {
        calls += "delete:$cardId"
        failOrRun("delete") { store.delete(cardId) }
    }

    private suspend fun failOrRun(
        operation: String,
        commit: suspend () -> Unit,
    ) {
        val next = planned.firstOrNull { it.operation == operation }
        if (next == null) {
            commit()
            return
        }
        planned.remove(next)
        if (next.afterCommit) commit()
        throw RemoteException(next.failure)
    }

    private data class Planned(
        val operation: String,
        val failure: RemoteFailure,
        val afterCommit: Boolean,
    )
}

/** Fuente de callbacks controlada a mano: emite cuando el test lo pide y cuenta listeners activos. */
class ManualSnapshotSource<T> : SnapshotSource<T> {
    var activeListeners = 0
        private set
    var removed = 0
        private set
    private var onValue: ((T) -> Unit)? = null
    private var onError: ((Throwable) -> Unit)? = null

    override fun listen(
        onValue: (T) -> Unit,
        onError: (Throwable) -> Unit,
    ): ListenerHandle {
        activeListeners++
        this.onValue = onValue
        this.onError = onError
        return ListenerHandle {
            activeListeners--
            removed++
        }
    }

    fun emit(value: T) {
        onValue?.invoke(value)
    }

    fun fail(error: Throwable) {
        onError?.invoke(error)
    }
}
