package com.awa2027.cloudboard.data.demo

import com.awa2027.cloudboard.domain.model.BoardCard
import com.awa2027.cloudboard.domain.model.BoardColumn
import com.awa2027.cloudboard.domain.model.SessionUser

/** Datos del modo demo (sin Firebase). Dos personas para probar las guardias de propiedad. */
object DemoData {
    val ana = SessionUser(uid = "demo-ana", displayName = "Ana López")
    val luis = SessionUser(uid = "demo-luis", displayName = "Luis Ixcot")
    val users: List<SessionUser> = listOf(ana, luis)

    val cards: List<BoardCard> =
        listOf(
            demoCard("c-101", "Diseñar pantalla de tablero", 5, BoardColumn.DONE, ana, 1_000),
            demoCard("c-102", "Escribir reglas de Firestore", 3, BoardColumn.IN_PROGRESS, ana, 3_000),
            demoCard("c-103", "Configurar Koin", 2, BoardColumn.IN_PROGRESS, luis, 2_000),
            demoCard("c-104", "Probar reintentos sin red", 8, BoardColumn.BACKLOG, luis, 4_000),
        )

    private fun demoCard(
        id: String,
        title: String,
        points: Int,
        column: BoardColumn,
        owner: SessionUser,
        updatedAtMs: Long,
    ) = BoardCard(id, title, points, column, owner.uid, owner.displayName, updatedAtMs)
}
