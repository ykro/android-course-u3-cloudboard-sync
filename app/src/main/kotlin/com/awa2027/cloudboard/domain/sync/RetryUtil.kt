package com.awa2027.cloudboard.domain.sync

import com.awa2027.cloudboard.domain.model.RemoteException
import kotlin.random.Random

/**
 * Variación aplicada a cada espera para que miles de clientes no reintenten al mismo tiempo.
 * Es inyectable: los tests usan una función fija y la app una con `Random`.
 */
fun interface Jitter {
    /** Recibe la espera ya acotada y el número de reintento (1, 2, ...); devuelve la espera a aplicar. */
    fun apply(
        delayMs: Long,
        retryNumber: Int,
    ): Long

    companion object {
        val None: Jitter = Jitter { delayMs, _ -> delayMs }

        /** Mitad fija más una parte aleatoria en [0, delay / 2]. Con un `Random` sembrado es determinista. */
        fun equal(random: Random): Jitter = Jitter { delayMs, _ -> delayMs / 2 + random.nextLong(delayMs / 2 + 1) }
    }
}

data class RetryPolicy(
    val maxAttempts: Int = 4,
    val initialDelayMs: Long = 200,
    val maxDelayMs: Long = 3_000,
    val jitter: Jitter = Jitter.None,
) {
    init {
        require(maxAttempts >= 1) { "maxAttempts debe ser al menos 1" }
        require(initialDelayMs >= 0) { "initialDelayMs no puede ser negativo" }
        require(maxDelayMs >= initialDelayMs) { "maxDelayMs debe ser mayor o igual que initialDelayMs" }
    }
}

/** Solo los fallos remotos marcados como transitorios merecen otro intento. */
fun isTransientFailure(error: Throwable): Boolean = error is RemoteException && error.failure.transient

object RetryUtil {
    /**
     * Espera antes del reintento número `retryNumber` (1 = el primer reintento):
     * `min(maxDelayMs, initialDelayMs * 2^(retryNumber - 1))`, luego el jitter,
     * y el resultado acotado a `[0, maxDelayMs]`. Nunca se desborda.
     */
    fun backoffDelay(
        policy: RetryPolicy,
        retryNumber: Int,
    ): Long = TODO("Reto, parte B: backoffDelay")

    /**
     * Ejecuta `block` hasta `maxAttempts` veces. Reintenta solo si `isTransient(error)` es verdadero,
     * esperando [backoffDelay] entre intentos. Un error no transitorio se relanza de inmediato,
     * la cancelación nunca se reintenta y, al agotar los intentos, se relanza el último error.
     * `block` recibe el número de intento (1, 2, ...).
     */
    suspend fun <T> retry(
        policy: RetryPolicy,
        isTransient: (Throwable) -> Boolean = ::isTransientFailure,
        block: suspend (attempt: Int) -> T,
    ): T = TODO("Reto, parte B: retry")
}
