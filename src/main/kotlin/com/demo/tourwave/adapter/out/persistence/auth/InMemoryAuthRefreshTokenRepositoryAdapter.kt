package com.demo.tourwave.adapter.out.persistence.auth

import com.demo.tourwave.application.auth.port.AuthRefreshTokenRepository
import com.demo.tourwave.domain.auth.AuthRefreshToken
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

@Repository
@Profile("!mysql & !mysql-test")
class InMemoryAuthRefreshTokenRepositoryAdapter : AuthRefreshTokenRepository {
    private val sequence = AtomicLong(0L)
    private val tokensById = ConcurrentHashMap<Long, AuthRefreshToken>()
    private val tokenIdByHash = ConcurrentHashMap<String, Long>()

    override fun save(token: AuthRefreshToken): AuthRefreshToken {
        val tokenId = token.id ?: sequence.incrementAndGet()
        val persisted = token.copy(id = tokenId)
        tokensById[tokenId] = persisted
        tokenIdByHash[persisted.tokenHash] = tokenId
        return persisted
    }

    override fun findByTokenHash(tokenHash: String): AuthRefreshToken? {
        return tokenIdByHash[tokenHash]?.let(tokensById::get)
    }

    override fun revokeAllByUserId(userId: Long, revokedAtUtc: Instant) {
        tokensById.entries.forEach { (id, token) ->
            if (token.userId == userId && token.revokedAtUtc == null) {
                tokensById[id] = token.copy(revokedAtUtc = revokedAtUtc)
            }
        }
    }

    override fun clear() {
        sequence.set(0L)
        tokensById.clear()
        tokenIdByHash.clear()
    }
}
