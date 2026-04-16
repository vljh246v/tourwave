package com.demo.tourwave.adapter.out.persistence.jpa.auth

import com.demo.tourwave.application.auth.port.UserActionTokenRepository
import com.demo.tourwave.domain.auth.UserActionToken
import com.demo.tourwave.domain.auth.UserActionTokenPurpose
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
@Profile("mysql", "mysql-test")
class JpaUserActionTokenRepositoryAdapter(
    private val userActionTokenJpaRepository: UserActionTokenJpaRepository,
) : UserActionTokenRepository {
    override fun save(token: UserActionToken): UserActionToken {
        return userActionTokenJpaRepository.save(token.toEntity()).toDomain()
    }

    override fun findByTokenHash(tokenHash: String): UserActionToken? {
        return userActionTokenJpaRepository.findByTokenHash(tokenHash)?.toDomain()
    }

    override fun findActiveByUserIdAndPurpose(
        userId: Long,
        purpose: UserActionTokenPurpose,
        now: Instant,
    ): List<UserActionToken> {
        return userActionTokenJpaRepository
            .findAllByUserIdAndPurposeAndConsumedAtUtcIsNullOrderByCreatedAtUtcDesc(userId, purpose)
            .map { it.toDomain() }
            .filter { it.isActive(now) }
    }

    override fun clear() {
        userActionTokenJpaRepository.deleteAllInBatch()
    }
}

private fun UserActionToken.toEntity(): UserActionTokenJpaEntity =
    UserActionTokenJpaEntity(
        id = id,
        userId = userId,
        tokenHash = tokenHash,
        purpose = purpose,
        expiresAtUtc = expiresAtUtc,
        createdAtUtc = createdAtUtc,
        consumedAtUtc = consumedAtUtc,
    )

private fun UserActionTokenJpaEntity.toDomain(): UserActionToken =
    UserActionToken(
        id = id,
        userId = userId,
        tokenHash = tokenHash,
        purpose = purpose,
        expiresAtUtc = expiresAtUtc,
        createdAtUtc = createdAtUtc,
        consumedAtUtc = consumedAtUtc,
    )
