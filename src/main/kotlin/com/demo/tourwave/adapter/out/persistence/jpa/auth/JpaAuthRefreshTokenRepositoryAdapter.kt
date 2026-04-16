package com.demo.tourwave.adapter.out.persistence.jpa.auth

import com.demo.tourwave.application.auth.port.AuthRefreshTokenRepository
import com.demo.tourwave.domain.auth.AuthRefreshToken
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
@Profile("mysql", "mysql-test")
class JpaAuthRefreshTokenRepositoryAdapter(
    private val authRefreshTokenJpaRepository: AuthRefreshTokenJpaRepository,
) : AuthRefreshTokenRepository {
    override fun save(token: AuthRefreshToken): AuthRefreshToken {
        return authRefreshTokenJpaRepository.save(token.toEntity()).toDomain()
    }

    override fun findByTokenHash(tokenHash: String): AuthRefreshToken? {
        return authRefreshTokenJpaRepository.findByTokenHash(tokenHash)?.toDomain()
    }

    override fun revokeAllByUserId(
        userId: Long,
        revokedAtUtc: Instant,
    ) {
        authRefreshTokenJpaRepository.findByUserId(userId)
            .forEach { entity ->
                if (entity.revokedAtUtc == null) {
                    authRefreshTokenJpaRepository.save(entity.copy(revokedAtUtc = revokedAtUtc))
                }
            }
    }

    override fun clear() {
        authRefreshTokenJpaRepository.deleteAllInBatch()
    }
}

private fun AuthRefreshToken.toEntity(): AuthRefreshTokenJpaEntity =
    AuthRefreshTokenJpaEntity(
        id = id,
        userId = userId,
        tokenHash = tokenHash,
        expiresAtUtc = expiresAtUtc,
        issuedAtUtc = issuedAtUtc,
        revokedAtUtc = revokedAtUtc,
    )

private fun AuthRefreshTokenJpaEntity.toDomain(): AuthRefreshToken =
    AuthRefreshToken(
        id = id,
        userId = userId,
        tokenHash = tokenHash,
        expiresAtUtc = expiresAtUtc,
        issuedAtUtc = issuedAtUtc,
        revokedAtUtc = revokedAtUtc,
    )
