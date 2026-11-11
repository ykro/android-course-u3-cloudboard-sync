package com.awa2027.cloudboard.domain.usecase

import com.awa2027.cloudboard.domain.model.BoardError
import com.awa2027.cloudboard.domain.model.SessionUser
import com.awa2027.cloudboard.domain.repository.AuthRepository

/** Primera guardia de toda escritura: sin sesión (o con uid vacío) no se toca el repositorio. */
internal fun AuthRepository.requireUser(): SessionUser =
    currentUser()?.takeIf { it.uid.isNotBlank() } ?: throw BoardError.NotSignedIn()
