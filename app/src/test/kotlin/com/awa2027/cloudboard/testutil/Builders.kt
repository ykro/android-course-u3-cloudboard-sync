package com.awa2027.cloudboard.testutil

import com.awa2027.cloudboard.data.ai.LocalBoardSummarizer
import com.awa2027.cloudboard.domain.repository.AuthRepository
import com.awa2027.cloudboard.domain.repository.BoardRepository
import com.awa2027.cloudboard.domain.support.Clock
import com.awa2027.cloudboard.domain.support.IdGenerator
import com.awa2027.cloudboard.domain.sync.RetryPolicy
import com.awa2027.cloudboard.domain.usecase.CreateCardUseCase
import com.awa2027.cloudboard.domain.usecase.DeleteCardUseCase
import com.awa2027.cloudboard.domain.usecase.ObserveBoardUseCase
import com.awa2027.cloudboard.domain.usecase.SummarizeBoardUseCase
import com.awa2027.cloudboard.domain.usecase.UpdateCardUseCase
import com.awa2027.cloudboard.ui.BoardViewModel

/** Arma el ViewModel a mano, igual que lo haría Koin, con dependencias de prueba. */
fun buildViewModel(
    board: BoardRepository,
    auth: AuthRepository,
    ids: IdGenerator = SequentialIds(),
    clock: Clock = FixedClock(),
    retry: RetryPolicy = TEST_RETRY,
): BoardViewModel {
    val local = LocalBoardSummarizer()
    return BoardViewModel(
        observeBoard = ObserveBoardUseCase(board),
        createCard = CreateCardUseCase(auth, board, ids, clock, retry),
        updateCard = UpdateCardUseCase(auth, board, clock, retry),
        deleteCard = DeleteCardUseCase(auth, board, retry),
        summarizeBoard = SummarizeBoardUseCase(primary = local, fallback = local),
        auth = auth,
    )
}
