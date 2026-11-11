package com.awa2027.cloudboard.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.awa2027.cloudboard.domain.model.BoardCard
import com.awa2027.cloudboard.domain.model.BoardColumn
import com.awa2027.cloudboard.domain.model.CardChanges
import com.awa2027.cloudboard.domain.model.CardDraft
import com.awa2027.cloudboard.domain.model.SessionUser
import com.awa2027.cloudboard.domain.repository.AuthRepository
import com.awa2027.cloudboard.domain.usecase.CreateCardUseCase
import com.awa2027.cloudboard.domain.usecase.DeleteCardUseCase
import com.awa2027.cloudboard.domain.usecase.ObserveBoardUseCase
import com.awa2027.cloudboard.domain.usecase.SummarizeBoardUseCase
import com.awa2027.cloudboard.domain.usecase.UpdateCardUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Estado inmutable de la pantalla. */
data class BoardUiState(
    val loading: Boolean = true,
    val user: SessionUser? = null,
    val onlyMine: Boolean = false,
    val columns: Map<BoardColumn, List<BoardCard>> = BoardColumn.entries.associateWith { emptyList() },
    val message: String? = null,
    val summary: String? = null,
) {
    val cards: List<BoardCard> get() = columns.values.flatten()
}

/**
 * Orquesta UseCases y expone un único StateFlow. No valida ni decide permisos:
 * eso vive en el dominio. Solo traduce resultados a estado.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BoardViewModel(
    private val observeBoard: ObserveBoardUseCase,
    private val createCard: CreateCardUseCase,
    private val updateCard: UpdateCardUseCase,
    private val deleteCard: DeleteCardUseCase,
    private val summarizeBoard: SummarizeBoardUseCase,
    private val auth: AuthRepository,
) : ViewModel() {
    private val onlyMine = MutableStateFlow(false)
    private val message = MutableStateFlow<String?>(null)
    private val summary = MutableStateFlow<String?>(null)

    private val cards: Flow<List<BoardCard>> =
        combine(auth.session, onlyMine) { user, mine -> if (mine) user?.uid else null }
            .distinctUntilChanged()
            .flatMapLatest { ownerId ->
                observeBoard(ownerId).catch { error ->
                    // Un listener que falla queda cerrado: mostramos el mensaje y el tablero vacío.
                    message.value = userMessageFor(error)
                    emit(emptyList())
                }
            }

    val uiState: StateFlow<BoardUiState> =
        combine(auth.session, onlyMine, cards, message, summary) { user, mine, list, msg, sum ->
            BoardUiState(
                loading = false,
                user = user,
                onlyMine = mine,
                columns = BoardColumn.entries.associateWith { column -> list.filter { it.column == column } },
                message = msg,
                summary = sum,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), BoardUiState())

    fun onCreateCard(
        title: String,
        points: Int,
        column: BoardColumn = BoardColumn.BACKLOG,
    ) = launchAction("Tarjeta creada.") { createCard(CardDraft(title, points, column)) }

    fun onMoveCard(
        cardId: String,
        to: BoardColumn,
    ) = launchAction("Tarjeta movida.") { updateCard(cardId, CardChanges(column = to)) }

    fun onDeleteCard(cardId: String) = launchAction("Tarjeta eliminada.") { deleteCard(cardId) }

    fun onSwitchUser() = launchAction("Sesión cambiada.") { auth.signIn() }

    fun onToggleOnlyMine() = onlyMine.update { !it }

    fun onSummarize() {
        viewModelScope.launch { summary.value = summarizeBoard(uiState.value.cards) }
    }

    fun onMessageShown() {
        message.value = null
    }

    private fun launchAction(
        success: String,
        action: suspend () -> Unit,
    ) {
        viewModelScope.launch { runAction(success, action) }
    }

    /**
     * Paso 4 del taller: ejecuta la acción; si termina, publica `success`; si falla con una excepción,
     * publica `userMessageFor(error)`. La cancelación se relanza. Las tarjetas visibles no se tocan.
     */
    internal suspend fun runAction(
        success: String,
        action: suspend () -> Unit,
    ) {
        TODO("Paso 4 del taller: runAction")
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
