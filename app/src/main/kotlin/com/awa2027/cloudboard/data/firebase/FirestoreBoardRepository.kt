package com.awa2027.cloudboard.data.firebase

import com.awa2027.cloudboard.data.ListenerHandle
import com.awa2027.cloudboard.data.SnapshotSource
import com.awa2027.cloudboard.data.asFlow
import com.awa2027.cloudboard.domain.model.BoardCard
import com.awa2027.cloudboard.domain.model.BoardColumn
import com.awa2027.cloudboard.domain.model.RemoteException
import com.awa2027.cloudboard.domain.model.RemoteFailure
import com.awa2027.cloudboard.domain.repository.BoardRepository
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.cancellation.CancellationException

/**
 * Implementación con Cloud Firestore: `boards/{boardId}/cards/{cardId}`.
 * No se ejecuta en los tests (no hay emulador); compila y se usa cuando existe `google-services.json`.
 */
class FirestoreBoardRepository(
    firestore: FirebaseFirestore,
    boardId: String,
    private val timeoutMs: Long = 10_000,
) : BoardRepository {
    private val cards = firestore.collection("boards").document(boardId).collection("cards")

    override fun observeCards(ownerId: String?): Flow<List<BoardCard>> {
        // whereEqualTo(ownerId) + orderBy(updatedAtMs) exige el índice compuesto de firestore.indexes.json.
        val filtered = if (ownerId == null) cards else cards.whereEqualTo(FIELD_OWNER_ID, ownerId)
        return QuerySource(filtered.orderBy(FIELD_UPDATED_AT, Query.Direction.DESCENDING)).asFlow()
    }

    override suspend fun findCard(cardId: String): BoardCard? =
        remote {
            cards.document(cardId).get().await()
        }.toBoardCard()

    // set() sobre un id estable es idempotente: si el ack se pierde y reintentamos, no hay duplicados.
    override suspend fun upsert(card: BoardCard) {
        remote { cards.document(card.id).set(card.toFirestoreMap()).await() }
    }

    override suspend fun delete(cardId: String) {
        remote { cards.document(cardId).delete().await() }
    }

    private suspend fun <T> remote(block: suspend () -> T): T =
        try {
            withTimeout(timeoutMs) { block() }
        } catch (timeout: TimeoutCancellationException) {
            // La escritura puede seguir en la cola local del SDK: por eso el reintento debe ser idempotente.
            throw RemoteException(RemoteFailure.DEADLINE_EXCEEDED, TIMEOUT_MESSAGE, timeout)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: RemoteException) {
            throw error
        } catch (error: Exception) {
            throw error.toRemoteException()
        }

    private class QuerySource(
        private val query: Query,
    ) : SnapshotSource<List<BoardCard>> {
        override fun listen(
            onValue: (List<BoardCard>) -> Unit,
            onError: (Throwable) -> Unit,
        ): ListenerHandle {
            val registration =
                query.addSnapshotListener { snapshot, error ->
                    when {
                        error != null -> onError(error.toRemoteException())
                        snapshot != null -> onValue(snapshot.documents.mapNotNull { it.toBoardCard() })
                    }
                }
            return ListenerHandle { registration.remove() }
        }
    }

    companion object {
        const val FIELD_OWNER_ID = "ownerId"
        const val FIELD_UPDATED_AT = "updatedAtMs"
        private const val TIMEOUT_MESSAGE = "Firestore no respondió a tiempo"
    }
}

internal fun BoardCard.toFirestoreMap(): Map<String, Any> =
    mapOf(
        "title" to title,
        "points" to points,
        "column" to column.name,
        FirestoreBoardRepository.FIELD_OWNER_ID to ownerId,
        "ownerName" to ownerName,
        FirestoreBoardRepository.FIELD_UPDATED_AT to updatedAtMs,
    )

/** Documento a tarjeta. Un documento incompleto o con columna desconocida se ignora. */
internal fun DocumentSnapshot.toBoardCard(): BoardCard? {
    if (!exists()) return null
    val column = BoardColumn.entries.firstOrNull { it.name == getString("column") } ?: return null
    return BoardCard(
        id = id,
        title = getString("title") ?: return null,
        points = getLong("points")?.toInt() ?: return null,
        column = column,
        ownerId = getString(FirestoreBoardRepository.FIELD_OWNER_ID) ?: return null,
        ownerName = getString("ownerName").orEmpty(),
        updatedAtMs = getLong(FirestoreBoardRepository.FIELD_UPDATED_AT) ?: 0L,
    )
}
