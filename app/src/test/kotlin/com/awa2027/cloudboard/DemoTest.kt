package com.awa2027.cloudboard

import app.cash.turbine.test
import com.awa2027.cloudboard.data.demo.DemoAuthRepository
import com.awa2027.cloudboard.data.demo.DemoData
import com.awa2027.cloudboard.data.demo.InMemoryBoardRepository
import com.awa2027.cloudboard.domain.model.BoardColumn
import com.awa2027.cloudboard.domain.usecase.ObserveBoardUseCase
import com.awa2027.cloudboard.testutil.MainDispatcherRule
import com.awa2027.cloudboard.testutil.buildViewModel
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals

/** Oficial. En verde desde el starter: el modo demo funciona sin Firebase ni red. */
class DemoTest {
    @get:Rule
    val mainRule = MainDispatcherRule()

    @Test
    fun `el tablero demo se observa ordenado por columna y de lo más reciente a lo más antiguo`() =
        runTest {
            val observe = ObserveBoardUseCase(InMemoryBoardRepository(DemoData.cards))

            observe().test {
                assertEquals(listOf("c-104", "c-102", "c-103", "c-101"), awaitItem().map { it.id })
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `el ViewModel publica el tablero demo y el resumen local sin red`() =
        runTest(mainRule.dispatcher) {
            val viewModel =
                buildViewModel(
                    board = InMemoryBoardRepository(DemoData.cards),
                    auth = DemoAuthRepository(DemoData.users),
                )

            viewModel.uiState.test {
                advanceUntilIdle()
                val state = expectMostRecentItem()
                assertEquals(DemoData.ana, state.user)
                assertEquals(listOf("c-102", "c-103"), state.columns.getValue(BoardColumn.IN_PROGRESS).map { it.id })

                viewModel.onSummarize()
                advanceUntilIdle()
                assertEquals(
                    "4 tarjetas y 18 puntos. Más carga en Por hacer (8 puntos).",
                    expectMostRecentItem().summary,
                )
                cancelAndIgnoreRemainingEvents()
            }
        }
}
