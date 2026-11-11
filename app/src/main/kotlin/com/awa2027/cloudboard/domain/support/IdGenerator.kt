package com.awa2027.cloudboard.domain.support

import java.util.UUID

/** Genera ids de tarjeta en el cliente, antes de escribir, para que reintentar no duplique. */
fun interface IdGenerator {
    fun newId(): String

    companion object {
        val Uuid: IdGenerator = IdGenerator { UUID.randomUUID().toString() }
    }
}
