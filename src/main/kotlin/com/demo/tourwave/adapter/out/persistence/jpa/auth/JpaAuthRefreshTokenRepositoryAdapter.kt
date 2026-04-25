package com.demo.tourwave.adapter.out.persistence.jpa.auth

import com.demo.tourwave.application.auth.port.AuthRefreshTokenRepository
import com.demo.tourwave.domain.auth.AuthRefreshToken
import com.demo.tourwave.domain.common.DomainException
import com.demo.tourwave.domain.common.ErrorCode
import jakarta.persistence.OptimisticLockException
import org.springframework.context.annotation.Profile
import org.springframework.orm.ObjectOptimisticLockingFailureException
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

    override fun rotate(
        token: AuthRefreshToken,
        revokedAtUtc: Instant,
    ): AuthRefreshToken {
        try {
            val revoked = token.copy(revokedAtUtc = revokedAtUtc)
            return authRefreshTokenJpaRepository.save(revoked.toEntity()).toDomain()
        } catch (e: OptimisticLockException) {
            throw DomainException(
                errorCode = ErrorCode.REFRESH_TOKEN_ROTATION_CONFLICT,
                status = 409,
                message = "refresh token rotation conflict",
            )
        } catch (e: ObjectOptimisticLockingFailureException) {
            throw DomainException(
                errorCode = ErrorCode.REFRESH_TOKEN_ROTATION_CONFLICT,
                status = 409,
                message = "refresh token rotation conflict",
            )
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
        version = version,
    )

private fun AuthRefreshTokenJpaEntity.toDomain(): AuthRefreshToken =
    AuthRefreshToken(
        id = id,
        userId = userId,
        tokenHash = tokenHash,
        expiresAtUtc = expiresAtUtc,
        issuedAtUtc = issuedAtUtc,
        revokedAtUtc = revokedAtUtc,
        version = version,
    )
