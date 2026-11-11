package com.awa2027.cloudboard.data.firebase

import com.awa2027.cloudboard.domain.model.RemoteException
import com.awa2027.cloudboard.domain.model.RemoteFailure
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.FirebaseFirestoreException

/** Traduce las excepciones del SDK de Firebase a [RemoteException] para que el dominio decida si reintentar. */
internal fun Throwable.toRemoteException(): RemoteException =
    when (this) {
        is RemoteException -> this
        is FirebaseFirestoreException -> RemoteException(code.toFailure(), message ?: code.name, this)
        is FirebaseNetworkException -> RemoteException(RemoteFailure.UNAVAILABLE, cause = this)
        is FirebaseTooManyRequestsException -> RemoteException(RemoteFailure.RESOURCE_EXHAUSTED, cause = this)
        is FirebaseAuthException -> RemoteException(RemoteFailure.UNAUTHENTICATED, cause = this)
        else -> RemoteException(RemoteFailure.UNKNOWN, message ?: "Fallo desconocido", this)
    }

private fun FirebaseFirestoreException.Code.toFailure(): RemoteFailure =
    when (this) {
        FirebaseFirestoreException.Code.UNAVAILABLE -> RemoteFailure.UNAVAILABLE

        FirebaseFirestoreException.Code.DEADLINE_EXCEEDED -> RemoteFailure.DEADLINE_EXCEEDED

        FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED -> RemoteFailure.RESOURCE_EXHAUSTED

        FirebaseFirestoreException.Code.ABORTED -> RemoteFailure.ABORTED

        FirebaseFirestoreException.Code.PERMISSION_DENIED -> RemoteFailure.PERMISSION_DENIED

        FirebaseFirestoreException.Code.UNAUTHENTICATED -> RemoteFailure.UNAUTHENTICATED

        FirebaseFirestoreException.Code.NOT_FOUND -> RemoteFailure.NOT_FOUND

        FirebaseFirestoreException.Code.INVALID_ARGUMENT,
        FirebaseFirestoreException.Code.FAILED_PRECONDITION,
        FirebaseFirestoreException.Code.OUT_OF_RANGE,
        -> RemoteFailure.INVALID_ARGUMENT

        else -> RemoteFailure.UNKNOWN
    }
