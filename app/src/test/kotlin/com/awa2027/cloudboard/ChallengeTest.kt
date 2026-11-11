package com.awa2027.cloudboard

import com.awa2027.cloudboard.domain.model.BoardColumn
import com.awa2027.cloudboard.domain.model.BoardError
import com.awa2027.cloudboard.domain.model.CardChanges
import com.awa2027.cloudboard.domain.model.CardDraft
import com.awa2027.cloudboard.domain.model.RemoteException
import com.awa2027.cloudboard.domain.model.RemoteFailure
import com.awa2027.cloudboard.domain.sync.RetryPolicy
import com.awa2027.cloudboard.domain.sync.RetryUtil
import com.awa2027.cloudboard.domain.usecase.CreateCardUseCase
import com.awa2027.cloudboard.domain.usecase.DeleteCardUseCase
import com.awa2027.cloudboard.domain.usecase.UpdateCardUseCase
import com.awa2027.cloudboard.testutil.ANA
import com.awa2027.cloudboard.testutil.FakeAuthRepository
import com.awa2027.cloudboard.testutil.FixedClock
import com.awa2027.cloudboard.testutil.LUIS
import com.awa2027.cloudboard.testutil.ScriptedBoardRepository
import com.awa2027.cloudboard.testutil.SequentialIds
import com.awa2027.cloudboard.testutil.TEST_RETRY
import com.awa2027.cloudboard.testutil.card
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/** Oficial. Tests públicos del reto: guardias de propiedad, reintentos y escrituras idempotentes. */
class ChallengeTest {
    // Parte A: guardias de propiedad

    @Test
    fun `borrar una tarjeta propia la quita del tablero`() =
        runTest {
            val board = ScriptedBoardRepository(listOf(card("c1", owner = ANA)))

            DeleteCardUseCase(FakeAuthRepository(ANA), board, TEST_RETRY)("c1")

            assertTrue(board.cards.isEmpty())
        }

    @Test
    fun `borrar una tarjeta ajena lanza NotOwner y la tarjeta sigue ahí`() =
        runTest {
            val board = ScriptedBoardRepository(listOf(card("c1", owner = ANA)))

            val error =
                assertFailsWith<BoardError.NotOwner> {
                    DeleteCardUseCase(FakeAuthRepository(LUIS), board, TEST_RETRY)("c1")
                }

            assertEquals("c1", error.cardId)
            assertEquals(listOf("c1"), board.cards.map { it.id })
        }

    @Test
    fun `mover una tarjeta propia cambia la columna y conserva el dueño`() =
        runTest {
            val board = ScriptedBoardRepository(listOf(card("c1", owner = ANA)))
            val update = UpdateCardUseCase(FakeAuthRepository(ANA), board, FixedClock(), TEST_RETRY)

            val moved = update("c1", CardChanges(column = BoardColumn.DONE))

            assertEquals(BoardColumn.DONE, moved.column)
            assertEquals(ANA.uid, moved.ownerId)
            assertEquals(BoardColumn.DONE, board.cards.single().column)
        }

    // Parte B: RetryUtil

    @Test
    fun `backoffDelay duplica la espera a partir de initialDelayMs`() {
        val policy = RetryPolicy(maxAttempts = 4, initialDelayMs = 100, maxDelayMs = 1_000)

        assertEquals(listOf(100L, 200L, 400L), (1..3).map { RetryUtil.backoffDelay(policy, it) })
    }

    @Test
    fun `retry reintenta fallos transitorios esperando en tiempo virtual`() =
        runTest {
            var calls = 0

            val result =
                RetryUtil.retry(RetryPolicy(maxAttempts = 4, initialDelayMs = 100, maxDelayMs = 1_000)) {
                    calls++
                    if (calls < 3) throw RemoteException(RemoteFailure.UNAVAILABLE)
                    "sincronizado"
                }

            assertEquals("sincronizado", result)
            assertEquals(3, calls)
            assertEquals(300, currentTime)
        }

    @Test
    fun `retry no reintenta un error de permisos`() =
        runTest {
            var calls = 0

            assertFailsWith<RemoteException> {
                RetryUtil.retry(TEST_RETRY) {
                    calls++
                    throw RemoteException(RemoteFailure.PERMISSION_DENIED)
                }
            }

            assertEquals(1, calls)
            assertEquals(0, currentTime)
        }

    // Parte C: escrituras idempotentes

    @Test
    fun `crear con un ack perdido no duplica la tarjeta`() =
        runTest {
            val board = ScriptedBoardRepository()
            board.failNext("upsert", RemoteFailure.DEADLINE_EXCEEDED, afterCommit = true)
            val ids = SequentialIds()

            val created =
                CreateCardUseCase(FakeAuthRepository(ANA), board, ids, FixedClock(), TEST_RETRY)(
                    CardDraft("Sincronizar sin duplicar", 5),
                )

            assertEquals(listOf(created.id), board.cards.map { it.id })
            assertEquals(1, ids.calls)
        }
}
