package com.awa2027.cloudboard.domain.usecase

import com.awa2027.cloudboard.domain.model.BoardCard
import com.awa2027.cloudboard.domain.repository.BoardSummarizer
import kotlin.coroutines.cancellation.CancellationException

/** Power-up opcional: intenta el resumidor principal (Gemini) y, si falla, usa el local sin red. */
class SummarizeBoardUseCase(
    private val primary: BoardSummarizer,
    private val fallback: BoardSummarizer,
) {
    suspend operator fun invoke(cards: List<BoardCard>): String =
        try {
            primary.summarize(cards)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            fallback.summarize(cards)
        }
}
