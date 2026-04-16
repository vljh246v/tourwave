package com.demo.tourwave.application.auth

import com.demo.tourwave.application.common.port.ActorRole
import com.demo.tourwave.domain.user.User

data class AuthResult(
    val accessToken: String,
    val refreshToken: String,
    val user: User,
)

data class AccessTokenClaims(
    val userId: Long,
    val roles: Set<ActorRole>,
    val orgId: Long?,
    val issuedAtEpochSeconds: Long,
    val expiresAtEpochSeconds: Long,
)
