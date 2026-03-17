package com.demo.tourwave.adapter.out.persistence.jpa.auth

import com.demo.tourwave.application.auth.port.UserActionTokenRepository
import com.demo.tourwave.domain.auth.UserActionToken
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Repository

@Repository
@Profile("mysql", "mysql-test")
class JpaUserActionTokenRepositoryAdapter(
    private val userActionTokenJpaRepository: UserActionTokenJpaRepository
) : UserActionTokenRepository {
    override fun save(token: UserActionToken): UserActionToken {
        return userActionTokenJpaRepository.save(token.toEntity()).toDomain()
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
        consumedAtUtc = consumedAtUtc
    )

private fun UserActionTokenJpaEntity.toDomain(): UserActionToken =
    UserActionToken(
        id = id,
        userId = userId,
        tokenHash = tokenHash,
        purpose = purpose,
        expiresAtUtc = expiresAtUtc,
        createdAtUtc = createdAtUtc,
        consumedAtUtc = consumedAtUtc
    )
