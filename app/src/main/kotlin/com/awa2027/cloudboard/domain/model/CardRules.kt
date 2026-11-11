package com.awa2027.cloudboard.domain.model

/**
 * Invariantes de una tarjeta. Se verifican en el dominio ANTES de persistir
 * y se repiten en firestore.rules para quien salte la app.
 */
object CardRules {
    const val TITLE_MIN = 3
    const val TITLE_MAX = 80
    val ALLOWED_POINTS: Set<Int> = setOf(1, 2, 3, 5, 8, 13)

    /** Quita espacios al inicio y al final y colapsa los espacios internos repetidos en uno. */
    fun normalizeTitle(raw: String): String = TODO("Paso 3 del taller: normaliza los espacios del título")

    /** Devuelve el título normalizado o lanza [BoardError.InvalidCard] con `Field.TITLE`. */
    fun requireValidTitle(raw: String): String = TODO("Paso 3 del taller: título normalizado de 3 a 80 caracteres")

    /** Devuelve los puntos si pertenecen a [ALLOWED_POINTS] o lanza [BoardError.InvalidCard] con `Field.POINTS`. */
    fun requireValidPoints(points: Int): Int = TODO("Paso 3 del taller: puntos dentro de ALLOWED_POINTS")
}
