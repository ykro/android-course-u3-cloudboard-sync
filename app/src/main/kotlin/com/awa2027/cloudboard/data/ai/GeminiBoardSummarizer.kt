package com.awa2027.cloudboard.data.ai

import com.awa2027.cloudboard.domain.model.BoardCard
import com.awa2027.cloudboard.domain.repository.BoardSummarizer
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend

/**
 * Power-up opcional con Firebase AI Logic. El modelo llega por configuración (`BuildConfig.GEMINI_MODEL`,
 * por defecto `gemini-3.8-flash`). Solo envía títulos, columnas y puntos: nunca uids ni nombres.
 * Si falla, `SummarizeBoardUseCase` usa el resumen local.
 */
class GeminiBoardSummarizer(
    private val modelName: String,
) : BoardSummarizer {
    override suspend fun summarize(cards: List<BoardCard>): String {
        val model = Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(modelName)
        val lines = cards.joinToString("\n") { "- [${it.column.label}] ${it.title} (${it.points} puntos)" }
        val prompt =
            "Resume en dos oraciones, en español, el estado de este tablero y señala el cuello de botella.\n$lines"
        return model.generateContent(prompt).text?.trim().orEmpty().ifEmpty {
            error("Gemini devolvió una respuesta vacía")
        }
    }
}
