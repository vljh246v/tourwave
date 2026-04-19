package com.demo.tourwave.application.auth.port

import com.demo.tourwave.domain.auth.UserActionToken
import com.demo.tourwave.domain.auth.UserActionTokenPurpose
import java.time.Instant

interface UserActionTokenRepository {
    fun save(token: UserActionToken): UserActionToken

    fun findByTokenHash(tokenHash: String): UserActionToken?

    fun findActiveByUserIdAndPurpose(
        userId: Long,
        purpose: UserActionTokenPurpose,
        now: Instant,
    ): List<UserActionToken>

    fun clear()
}
