package com.demo.tourwave.adapter.out.persistence.auth

import com.demo.tourwave.application.auth.port.UserActionTokenRepository
import com.demo.tourwave.domain.auth.UserActionToken
import com.demo.tourwave.domain.auth.UserActionTokenPurpose
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

@Repository
@Profile("!mysql & !mysql-test")
class InMemoryUserActionTokenRepositoryAdapter : UserActionTokenRepository {
    private val sequence = AtomicLong(0L)
    private val tokensById = ConcurrentHashMap<Long, UserActionToken>()

    override fun save(token: UserActionToken): UserActionToken {
        val tokenId = token.id ?: sequence.incrementAndGet()
        val persisted = token.copy(id = tokenId)
        tokensById[tokenId] = persisted
        return persisted
    }

    override fun findByTokenHash(tokenHash: String): UserActionToken? {
        return tokensById.values.firstOrNull { it.tokenHash == tokenHash }
    }

    override fun findActiveByUserIdAndPurpose(
        userId: Long,
        purpose: UserActionTokenPurpose,
        now: Instant
    ): List<UserActionToken> {
        return tokensById.values
            .filter { it.userId == userId && it.purpose == purpose && it.isActive(now) }
            .sortedByDescending { it.createdAtUtc }
    }

    override fun clear() {
        sequence.set(0L)
        tokensById.clear()
    }
}
