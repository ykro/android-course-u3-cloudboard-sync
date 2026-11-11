package com.awa2027.cloudboard.domain.support

/** Reloj inyectable: los tests usan uno fijo. */
fun interface Clock {
    fun nowMs(): Long

    companion object {
        val System: Clock = Clock { java.lang.System.currentTimeMillis() }
    }
}
