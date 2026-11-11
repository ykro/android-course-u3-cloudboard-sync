package com.awa2027.cloudboard.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.awa2027.cloudboard.data.demo.DemoData
import com.awa2027.cloudboard.di.AppMode
import com.awa2027.cloudboard.domain.model.BoardCard
import com.awa2027.cloudboard.domain.model.BoardColumn
import com.awa2027.cloudboard.domain.model.CardRules
import com.awa2027.cloudboard.ui.theme.CloudBoardTheme
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

/** Punto de entrada con dependencias: obtiene el ViewModel de Koin y conecta eventos. */
@Composable
fun BoardRoute(
    viewModel: BoardViewModel = koinViewModel(),
    mode: AppMode = koinInject(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    BoardScreen(
        state = state,
        cloudMode = mode.cloud,
        actions =
            BoardActions(
                onCreate = viewModel::onCreateCard,
                onMove = viewModel::onMoveCard,
                onDelete = viewModel::onDeleteCard,
                onSwitchUser = viewModel::onSwitchUser,
                onToggleOnlyMine = viewModel::onToggleOnlyMine,
                onSummarize = viewModel::onSummarize,
                onMessageShown = viewModel::onMessageShown,
            ),
    )
}

/** Eventos de la pantalla. La pantalla no conoce UseCases ni Firebase. */
data class BoardActions(
    val onCreate: (title: String, points: Int, column: BoardColumn) -> Unit = { _, _, _ -> },
    val onMove: (cardId: String, to: BoardColumn) -> Unit = { _, _ -> },
    val onDelete: (cardId: String) -> Unit = {},
    val onSwitchUser: () -> Unit = {},
    val onToggleOnlyMine: () -> Unit = {},
    val onSummarize: () -> Unit = {},
    val onMessageShown: () -> Unit = {},
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoardScreen(
    state: BoardUiState,
    cloudMode: Boolean,
    actions: BoardActions,
) {
    val snackbar = remember { SnackbarHostState() }
    var showNewCard by remember { mutableStateOf(false) }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            actions.onMessageShown()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(if (cloudMode) "CloudBoard (Firebase)" else "CloudBoard (modo demo)") })
        },
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showNewCard = true }) { Text("Nueva") }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item { SessionBar(state, actions) }
            state.summary?.let { summary -> item { Text(summary, style = MaterialTheme.typography.bodyMedium) } }
            BoardColumn.entries.forEach { column ->
                val cards = state.columns[column].orEmpty()
                item { Text("${column.label} (${cards.size})", style = MaterialTheme.typography.titleMedium) }
                items(cards, key = { it.id }) { card ->
                    CardItem(card = card, isMine = card.ownerId == state.user?.uid, actions = actions)
                }
            }
        }
    }

    if (showNewCard) {
        NewCardDialog(
            onDismiss = { showNewCard = false },
            onConfirm = { title, points ->
                actions.onCreate(title, points, BoardColumn.BACKLOG)
                showNewCard = false
            },
        )
    }
}

@Composable
private fun SessionBar(
    state: BoardUiState,
    actions: BoardActions,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Sesión: ${state.user?.displayName ?: "sin sesión"}")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = state.onlyMine, onClick = actions.onToggleOnlyMine, label = { Text("Solo mías") })
            TextButton(onClick = actions.onSwitchUser) { Text("Cambiar usuario") }
            TextButton(onClick = actions.onSummarize) { Text("Resumir") }
        }
    }
}

@Composable
private fun CardItem(
    card: BoardCard,
    isMine: Boolean,
    actions: BoardActions,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(card.title, style = MaterialTheme.typography.titleSmall)
            Text("${card.points} puntos · ${card.ownerName}${if (isMine) " (tú)" else ""}")
            Row {
                // Los botones se muestran a todos a propósito: la UI no es la barrera de seguridad.
                val next = BoardColumn.entries.getOrNull(card.column.ordinal + 1)
                if (next !=
                    null
                ) {
                    TextButton(onClick = { actions.onMove(card.id, next) }) { Text("Mover a ${next.label}") }
                }
                TextButton(onClick = { actions.onDelete(card.id) }) { Text("Borrar") }
            }
        }
    }
}

@Composable
private fun NewCardDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Int) -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var points by remember { mutableIntStateOf(CardRules.ALLOWED_POINTS.first()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nueva tarjeta") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Título") })
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    CardRules.ALLOWED_POINTS.forEach { value ->
                        FilterChip(selected = points == value, onClick = { points = value }, label = { Text("$value") })
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(title, points) }) { Text("Crear") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

@Preview(showBackground = true)
@Composable
private fun BoardScreenPreview() {
    CloudBoardTheme {
        BoardScreen(
            state =
                BoardUiState(
                    loading = false,
                    user = DemoData.ana,
                    columns = BoardColumn.entries.associateWith { c -> DemoData.cards.filter { it.column == c } },
                ),
            cloudMode = false,
            actions = BoardActions(),
        )
    }
}
