package com.awa2027.cloudboard

import app.cash.turbine.test
import com.awa2027.cloudboard.data.asFlow
import com.awa2027.cloudboard.data.demo.DemoAuthRepository
import com.awa2027.cloudboard.data.demo.InMemoryBoardRepository
import com.awa2027.cloudboard.di.appModules
import com.awa2027.cloudboard.domain.model.BoardColumn
import com.awa2027.cloudboard.domain.model.BoardError
import com.awa2027.cloudboard.domain.model.CardDraft
import com.awa2027.cloudboard.domain.model.RemoteException
import com.awa2027.cloudboard.domain.model.RemoteFailure
import com.awa2027.cloudboard.domain.repository.AuthRepository
import com.awa2027.cloudboard.domain.repository.BoardRepository
import com.awa2027.cloudboard.domain.usecase.CreateCardUseCase
import com.awa2027.cloudboard.testutil.ANA
import com.awa2027.cloudboard.testutil.FakeAuthRepository
import com.awa2027.cloudboard.testutil.FixedClock
import com.awa2027.cloudboard.testutil.MainDispatcherRule
import com.awa2027.cloudboard.testutil.ManualSnapshotSource
import com.awa2027.cloudboard.testutil.ScriptedBoardRepository
import com.awa2027.cloudboard.testutil.SequentialIds
import com.awa2027.cloudboard.testutil.TEST_RETRY
import com.awa2027.cloudboard.testutil.buildViewModel
import com.awa2027.cloudboard.testutil.card
import com.awa2027.cloudboard.ui.BoardViewModel
import com.awa2027.cloudboard.ui.userMessageFor
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.koin.dsl.koinApplication
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.test.fail

/** Oficial. Taller guiado, pasos 2 a 6. */
class WorkshopTest {
    @get:Rule
    val mainRule = MainDispatcherRule()

    private fun createUseCase(
        board: BoardRepository,
        auth: AuthRepository = FakeAuthRepository(ANA),
        ids: SequentialIds = SequentialIds(),
        clock: FixedClock = FixedClock(),
    ) = CreateCardUseCase(auth, board, ids, clock, TEST_RETRY)

    // Paso 2: callbackFlow + awaitClose

    @Test
    fun `paso 2 - asFlow emite cada valor del listener en orden`() =
        runTest {
            val source = ManualSnapshotSource<Int>()

            source.asFlow().test {
                source.emit(1)
                source.emit(2)
                assertEquals(1, awaitItem())
                assertEquals(2, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `paso 2 - cancelar el colector quita el listener con awaitClose`() =
        runTest {
            val source = ManualSnapshotSource<Int>()

            source.asFlow().test {
                assertEquals(1, source.activeListeners)
                cancel()
            }

            assertEquals(0, source.activeListeners)
            assertEquals(1, source.removed)
        }

    @Test
    fun `paso 2 - un error del SDK cierra el Flow con esa excepción y quita el listener`() =
        runTest {
            val source = ManualSnapshotSource<Int>()

            source.asFlow().test {
                source.fail(RemoteException(RemoteFailure.PERMISSION_DENIED))
                val error = awaitError()
                assertIs<RemoteException>(error)
                assertEquals(RemoteFailure.PERMISSION_DENIED, error.failure)
            }

            assertEquals(0, source.activeListeners)
        }

    // Paso 3: UseCase con invariantes antes de persistir

    @Test
    fun `paso 3 - crear normaliza el título y toma el dueño de la sesión`() =
        runTest {
            val board = InMemoryBoardRepository()
            val clock = FixedClock(now = 77_000)

            val created =
                createUseCase(board, clock = clock)(
                    CardDraft(title = "  Revisar   reglas  ", points = 5, column = BoardColumn.IN_PROGRESS),
                )

            assertEquals("Revisar reglas", created.title)
            assertEquals("uid-ana", created.ownerId)
            assertEquals("Ana", created.ownerName)
            assertEquals("id-1", created.id)
            assertEquals(77_000, created.updatedAtMs)
            assertEquals(listOf(created), board.snapshot)
        }

    @Test
    fun `paso 3 - crear sin sesión lanza NotSignedIn y no escribe`() =
        runTest {
            val board = ScriptedBoardRepository()

            assertFailsWith<BoardError.NotSignedIn> {
                createUseCase(board, auth = FakeAuthRepository(null))(CardDraft("Tarjeta válida", 3))
            }

            assertEquals(0, board.writes)
        }

    @Test
    fun `paso 3 - un título inválido lanza InvalidCard TITLE antes de escribir`() =
        runTest {
            val board = mockk<BoardRepository>()
            val create = createUseCase(board)

            for (title in listOf("ab", "     ", "x".repeat(81))) {
                val error = assertFailsWith<BoardError.InvalidCard> { create(CardDraft(title, 3)) }
                assertEquals(BoardError.Field.TITLE, error.field)
            }

            coVerify(exactly = 0) { board.upsert(any()) }
        }

    @Test
    fun `paso 3 - puntos fuera de 1, 2, 3, 5, 8, 13 lanzan InvalidCard POINTS`() =
        runTest {
            val board = mockk<BoardRepository>()
            val create = createUseCase(board)

            for (points in listOf(0, 4, 21)) {
                val error = assertFailsWith<BoardError.InvalidCard> { create(CardDraft("Tarjeta válida", points)) }
                assertEquals(BoardError.Field.POINTS, error.field)
            }

            coVerify(exactly = 0) { board.upsert(any()) }
        }

    // Paso 4: ViewModel delgado

    @Test
    fun `paso 4 - crear desde el ViewModel publica el mensaje y la tarjeta aparece`() =
        runTest(mainRule.dispatcher) {
            val viewModel = buildViewModel(InMemoryBoardRepository(), FakeAuthRepository(ANA))

            viewModel.uiState.test {
                advanceUntilIdle()
                viewModel.onCreateCard("Diseñar login", 3)
                advanceUntilIdle()

                val state = expectMostRecentItem()
                assertEquals("Tarjeta creada.", state.message)
                assertEquals(listOf("Diseñar login"), state.columns.getValue(BoardColumn.BACKLOG).map { it.title })
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `paso 4 - un borrador inválido publica el mensaje de dominio y conserva las tarjetas`() =
        runTest(mainRule.dispatcher) {
            val board = InMemoryBoardRepository(listOf(card("a"), card("b")))
            val viewModel = buildViewModel(board, FakeAuthRepository(ANA))

            viewModel.uiState.test {
                advanceUntilIdle()
                viewModel.onCreateCard("x", 3)
                advanceUntilIdle()

                val state = expectMostRecentItem()
                assertEquals("El título debe tener entre 3 y 80 caracteres.", state.message)
                assertEquals(2, state.cards.size)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `paso 4 - userMessageFor traduce cada error tipado`() {
        val expected =
            mapOf(
                BoardError.NotSignedIn() to "Inicia sesión para editar el tablero.",
                BoardError.CardNotFound("c") to "La tarjeta ya no existe.",
                BoardError.NotOwner("c", "u") to "Solo quien creó la tarjeta puede modificarla.",
                BoardError.InvalidCard(BoardError.Field.POINTS, "x") to "Los puntos deben ser 1, 2, 3, 5, 8 o 13.",
                RemoteException(RemoteFailure.UNAVAILABLE) to "Sin conexión con la nube. Intenta de nuevo.",
                RemoteException(RemoteFailure.PERMISSION_DENIED) to "La nube rechazó la operación por permisos.",
                RemoteException(RemoteFailure.UNAUTHENTICATED) to "Tu sesión expiró. Vuelve a iniciar sesión.",
                IllegalStateException("x") to "Ocurrió un error inesperado.",
            )

        expected.forEach { (error, message) -> assertEquals(message, userMessageFor(error), error.toString()) }
    }

    @Test
    fun `paso 4 - cancelar una acción en curso no publica ningún mensaje`() =
        runTest(mainRule.dispatcher) {
            val viewModel = buildViewModel(InMemoryBoardRepository(), FakeAuthRepository(ANA))

            viewModel.uiState.test {
                advanceUntilIdle()
                val job = launch { viewModel.runAction("Listo.") { awaitCancellation() } }
                advanceUntilIdle()
                job.cancel()
                advanceUntilIdle()

                assertNull(expectMostRecentItem().message)
                cancelAndIgnoreRemainingEvents()
            }
        }

    // Paso 5: inyección con Koin 4

    @Test
    fun `paso 5 - el grafo demo de Koin resuelve el ViewModel sin Firebase`() {
        val koin = koinApplication { modules(appModules(cloudEnabled = false)) }.koin
        try {
            koin.get<BoardViewModel>()
            assertIs<DemoAuthRepository>(koin.get<AuthRepository>())
            assertIs<InMemoryBoardRepository>(koin.get<BoardRepository>())
        } finally {
            koin.close()
        }
    }

    @Test
    fun `paso 5 - el repositorio demo es una sola instancia para todo el grafo`() {
        val koin = koinApplication { modules(appModules(cloudEnabled = false)) }.koin
        try {
            assertSame(koin.get<BoardRepository>(), koin.get<BoardRepository>())
            assertSame(koin.get<AuthRepository>(), koin.get<AuthRepository>())
        } finally {
            koin.close()
        }
    }

    // Paso 6: reglas de Firestore. Verificación estática sin emulador: lee firestore.rules como el
    // servidor, que permite una operación si CUALQUIER regla allow que coincide la permite. Por eso
    // revisa todas las reglas de cada operación, en todas las rutas, y no solo la primera.

    private class AllowRule(
        val path: String,
        val operations: List<String>,
        val condition: String?,
    )

    private class RulesFunction(
        val params: List<String>,
        val body: String,
    )

    private class ParsedRules(
        val allows: List<AllowRule>,
        val functions: Map<String, RulesFunction>,
    )

    private val parsedRules: ParsedRules by lazy {
        val path = System.getProperty("cloudboard.rulesFile") ?: fail("Falta la propiedad cloudboard.rulesFile")
        parseRules(stripComments(File(path).readText()))
    }

    /** Quita los comentarios de línea y de bloque sin tocar el contenido de las cadenas. */
    private fun stripComments(source: String): String {
        val out = StringBuilder()
        var i = 0
        var quote: Char? = null
        while (i < source.length) {
            val c = source[i]
            when {
                quote != null -> {
                    out.append(c)
                    if (c == '\\' && i + 1 < source.length) {
                        out.append(source[i + 1])
                        i++
                    } else if (c == quote) {
                        quote = null
                    }
                    i++
                }

                c == '\'' || c == '"' -> {
                    quote = c
                    out.append(c)
                    i++
                }

                source.startsWith("//", i) -> {
                    i = source.indexOf('\n', i).let { if (it < 0) source.length else it }
                }

                source.startsWith("/*", i) -> {
                    val end = source.indexOf("*/", i + 2)
                    if (end < 0) fail("Comentario /* sin cerrar en firestore.rules")
                    out.append(' ')
                    i = end + 2
                }

                else -> {
                    out.append(c)
                    i++
                }
            }
        }
        return out.toString()
    }

    /** Posiciones de [token] fuera de cadenas, paréntesis, corchetes y llaves, desde [from]. */
    private fun topLevelIndexes(
        text: String,
        token: String,
        from: Int = 0,
    ): List<Int> {
        val found = mutableListOf<Int>()
        var depth = 0
        var quote: Char? = null
        var i = from
        while (i < text.length) {
            val c = text[i]
            if (quote != null) {
                if (c == '\\') {
                    i++
                } else if (c == quote) {
                    quote = null
                }
            } else if (c == '\'' || c == '"') {
                quote = c
            } else if (depth == 0 && text.startsWith(token, i)) {
                found += i
                i += token.length
                continue
            } else if (c == '(' || c == '[' || c == '{') {
                depth++
            } else if (c == ')' || c == ']' || c == '}') {
                depth--
            }
            i++
        }
        return found
    }

    private fun matchingBrace(
        text: String,
        open: Int,
    ): Int = topLevelIndexes(text, "}", open + 1).firstOrNull() ?: fail("Falta cerrar una llave en firestore.rules")

    private fun parseRules(text: String): ParsedRules {
        val allows = mutableListOf<AllowRule>()
        val functions = mutableMapOf<String, RulesFunction>()
        val scopes = ArrayDeque<String>()
        var i = 0

        fun keywordAt(keyword: String) =
            text.startsWith(keyword, i) && text.getOrNull(i + keyword.length)?.isWhitespace() == true
        while (i < text.length) {
            val c = text[i]
            when {
                c.isWhitespace() -> {
                    i++
                }

                c == '}' -> {
                    if (scopes.isEmpty()) fail("Hay una llave de cierre de más en firestore.rules")
                    scopes.removeLast()
                    i++
                }

                text.startsWith("rules_version", i) -> {
                    i = (topLevelIndexes(text, ";", i).firstOrNull() ?: fail("Falta ';' tras rules_version")) + 1
                }

                keywordAt("service") || keywordAt("match") -> {
                    // La llave del bloque es la primera que no abre un comodín de la ruta (/{boardId}).
                    val open =
                        (i until text.length).firstOrNull { text[it] == '{' && text[it - 1] != '/' }
                            ?: fail("Falta '{' en firestore.rules")
                    val header = text.substring(i, open).trim()
                    scopes.addLast(if (header.startsWith("match")) header.removePrefix("match").trim() else "")
                    i = open + 1
                }

                keywordAt("function") -> {
                    val open =
                        text.indexOf('{', i).takeIf { it >= 0 } ?: fail("Falta '{' en una función de firestore.rules")
                    val header = text.substring(i + "function".length, open).trim()
                    val close = matchingBrace(text, open)
                    val body = text.substring(open + 1, close)
                    val returnAt =
                        topLevelIndexes(body, "return ").lastOrNull() ?: fail("La función $header no tiene return")
                    val expression =
                        body
                            .substring(returnAt + "return ".length)
                            .trim()
                            .removeSuffix(";")
                            .trim()
                    val params =
                        header
                            .substringAfter('(')
                            .substringBeforeLast(')')
                            .split(',')
                            .map { it.trim() }
                            .filter { it.isNotEmpty() }
                    functions[header.substringBefore('(').trim()] = RulesFunction(params, expression)
                    i = close + 1
                }

                keywordAt("allow") -> {
                    val end =
                        topLevelIndexes(text, ";", i).firstOrNull() ?: fail("Falta ';' al final de una regla allow")
                    val statement = text.substring(i + "allow".length, end).trim()
                    val operations = statement.substringBefore(':').split(',').map { it.trim() }
                    val condition =
                        if (':' in statement) {
                            statement
                                .substringAfter(
                                    ':',
                                ).trim()
                                .let { if (it.startsWith("if ")) it.removePrefix("if ").trim() else it }
                        } else {
                            null
                        }
                    allows += AllowRule(scopes.joinToString(""), operations, condition)
                    i = end + 1
                }

                else -> {
                    fail("No se pudo leer firestore.rules cerca de: ${text.substring(i).take(40)}")
                }
            }
        }
        if (scopes.isNotEmpty()) fail("Falta cerrar un bloque en firestore.rules")
        return ParsedRules(allows, functions)
    }

    private fun compact(expression: String) = expression.replace(Regex("\\s+"), "")

    private fun stripOuterParens(expression: String): String {
        var current = expression.trim()
        while (current.startsWith("(") && topLevelIndexes(current, ")", 1).firstOrNull() == current.length - 1) {
            current = current.substring(1, current.length - 1).trim()
        }
        return current
    }

    private fun split(
        expression: String,
        token: String,
    ): List<String> {
        val cuts = topLevelIndexes(expression, token)
        val starts = listOf(0) + cuts.map { it + token.length }
        val ends = cuts + expression.length
        return starts.zip(ends) { start, end -> expression.substring(start, end) }
    }

    /**
     * Partes unidas con `&&` en el nivel superior de la condición, con las funciones sin parámetros
     * expandidas. Si la condición tiene `||` o `?:` en el nivel superior no es una conjunción y se
     * devuelve entera: ninguna de sus partes está garantizada.
     */
    private fun ParsedRules.conjuncts(
        expression: String,
        depth: Int = 0,
    ): List<String> {
        val current = stripOuterParens(expression)
        if (split(current, "||").size > 1 || split(current, "?").size > 1) return listOf(compact(current))
        return split(current, "&&").flatMap { part ->
            val piece = stripOuterParens(part)
            val call = Regex("^([A-Za-z_][A-Za-z0-9_]*)\\(\\)$").find(piece)
            val function = call?.let { functions[it.groupValues[1]] }
            if (function != null && function.params.isEmpty() && depth < 8) {
                conjuncts(function.body, depth + 1)
            } else {
                listOf(compact(piece))
            }
        }
    }

    /** La condición y el cuerpo de todas las funciones que usa, directa o indirectamente. */
    private fun ParsedRules.expanded(condition: String): String {
        val seen = mutableSetOf<String>()
        val pending = ArrayDeque(listOf(condition))
        val text = StringBuilder()
        while (pending.isNotEmpty()) {
            val next = pending.removeFirst()
            text.append(next).append('\n')
            functions.forEach { (name, function) ->
                if (name !in seen && Regex("\\b$name\\s*\\(").containsMatchIn(next)) {
                    seen += name
                    pending.addLast(function.body)
                }
            }
        }
        return text.toString()
    }

    private val writeOperations = setOf("write", "create", "update", "delete")

    private fun normalizedPath(path: String) = compact(path).replace(Regex("\\{[A-Za-z_][A-Za-z0-9_]*\\}"), "{*}")

    private val cardsPath = "/databases/{*}/documents/boards/{*}/cards/{*}"

    /** Todas las reglas, de cualquier ruta, que permiten [operation] (directamente o con `write`). */
    private fun rulesFor(operation: String): List<AllowRule> {
        val rules = parsedRules.allows.filter { operation in it.operations || "write" in it.operations }
        assertTrue(rules.isNotEmpty(), "No hay una regla 'allow $operation'")
        return rules
    }

    private fun AllowRule.requires(
        left: String,
        right: String,
    ): Boolean {
        val parts = parsedRules.conjuncts(condition ?: "true")
        return compact("$left == $right") in parts || compact("$right == $left") in parts
    }

    private fun AllowRule.validatesDocument(): Boolean =
        condition != null && "keys().hasOnly(" in compact(parsedRules.expanded(condition))

    @Test
    fun `paso 6 - ninguna regla abre escrituras con write, if true, OR ni fuera de la ruta de tarjetas`() {
        val rules = parsedRules.allows
        assertTrue(rules.isNotEmpty(), "firestore.rules no tiene reglas allow")
        rules.forEach { rule ->
            assertFalse(
                "write" in rule.operations,
                "Usa create, update y delete por separado en lugar de 'allow write'",
            )
            assertTrue(
                rule.condition != null && compact(rule.condition) != "true",
                "La regla 'allow ${rule.operations.joinToString()}' no tiene condición o usa 'if true': " +
                    "abre el tablero a cualquiera",
            )
            if (rule.operations.any { it in writeOperations }) {
                assertEquals(
                    cardsPath,
                    normalizedPath(rule.path),
                    "Solo match /boards/{boardId}/cards/{cardId} puede permitir escrituras; " +
                        "Firestore permite la operación si cualquier regla que coincide la permite (${rule.path})",
                )
                assertFalse(
                    "||" in parsedRules.expanded(rule.condition.orEmpty()),
                    "Las reglas de escritura no usan ||: la alternativa deja pasar la escritura " +
                        "aunque falle la condición de dueño",
                )
            }
        }
    }

    @Test
    fun `paso 6 - crear exige que el ownerId nuevo sea el uid de la sesión`() {
        rulesFor("create").forEach { rule ->
            assertTrue(
                rule.requires("request.resource.data.ownerId", "request.auth.uid"),
                "Toda regla de create exige request.resource.data.ownerId == request.auth.uid, unida con &&",
            )
            assertTrue(
                rule.validatesDocument(),
                "Toda regla de create valida el documento (keys().hasOnly, como validCard)",
            )
        }
    }

    @Test
    fun `paso 6 - editar exige ser el dueño actual y no permite cambiar ownerId`() {
        rulesFor("update").forEach { rule ->
            assertTrue(
                rule.requires("resource.data.ownerId", "request.auth.uid"),
                "Toda regla de update exige resource.data.ownerId == request.auth.uid, unida con &&",
            )
            assertTrue(
                rule.requires("request.resource.data.ownerId", "resource.data.ownerId"),
                "Falta impedir el cambio de dueño: request.resource.data.ownerId == resource.data.ownerId",
            )
            assertTrue(
                rule.validatesDocument(),
                "Toda regla de update valida el documento (keys().hasOnly, como validCard)",
            )
        }
    }

    @Test
    fun `paso 6 - borrar exige ser el dueño actual`() {
        rulesFor("delete").forEach { rule ->
            assertTrue(
                rule.requires("resource.data.ownerId", "request.auth.uid"),
                "Toda regla de delete exige resource.data.ownerId == request.auth.uid, unida con &&",
            )
        }
    }
}
