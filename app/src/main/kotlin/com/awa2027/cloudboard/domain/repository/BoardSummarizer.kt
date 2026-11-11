package com.awa2027.cloudboard.domain.repository

import com.awa2027.cloudboard.domain.model.BoardCard

/** Power-up opcional: resume el tablero. La implementación local no usa red. */
fun interface BoardSummarizer {
    suspend fun summarize(cards: List<BoardCard>): String
}
