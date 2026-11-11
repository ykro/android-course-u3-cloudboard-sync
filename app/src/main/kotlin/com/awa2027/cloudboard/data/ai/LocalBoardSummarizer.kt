package com.awa2027.cloudboard.data.ai

import com.awa2027.cloudboard.domain.model.BoardCard
import com.awa2027.cloudboard.domain.repository.BoardSummarizer

/** Resumen determinista, sin red. Es el modo demo y el respaldo cuando Gemini no responde. */
class LocalBoardSummarizer : BoardSummarizer {
    override suspend fun summarize(cards: List<BoardCard>): String {
        if (cards.isEmpty()) return "El tablero está vacío."
        val busiest = cards.groupBy { it.column }.maxBy { (_, list) -> list.sumOf { it.points } }
        val busiestPoints = busiest.value.sumOf { it.points }
        val total = cards.sumOf { it.points }
        return "${cards.size} tarjetas y $total puntos. Más carga en ${busiest.key.label} ($busiestPoints puntos)."
    }
}
