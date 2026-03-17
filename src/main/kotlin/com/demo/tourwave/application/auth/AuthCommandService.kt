package com.demo.tourwave.application.auth

import com.demo.tourwave.application.auth.port.PasswordHasher
import com.demo.tourwave.application.auth.port.UserActionTokenRepository
import com.demo.tourwave.application.common.port.ActorRole
import com.demo.tourwave.application.user.port.UserRepository
import com.demo.tourwave.domain.auth.UserActionToken
import com.demo.tourwave.domain.auth.UserActionTokenPurpose
import com.demo.tourwave.domain.common.DomainException
import com.demo.tourwave.domain.common.ErrorCode
import com.demo.tourwave.domain.user.User
import com.demo.tourwave.domain.user.UserStatus
import java.security.MessageDigest
import java.time.Clock
import java.util.Base64
import java.util.UUID

class AuthCommandService(
    private val userRepository: UserRepository,
    private val passwordHasher: PasswordHasher,
    private val jwtTokenService: JwtTokenService,
    private val authTokenLifecycleService: AuthTokenLifecycleService,
    private val userActionTokenRepository: UserActionTokenRepository,
    private val clock: Clock
) {
    private val base64Encoder = Base64.getUrlEncoder().withoutPadding()

    fun signup(displayName: String, email: String, password: String): AuthResult {
        val normalizedEmail = requireValidEmail(email)
        val normalizedDisplayName = requireValidDisplayName(displayName)
        val normalizedPassword = requireValidPassword(password)
        if (userRepository.findByEmail(normalizedEmail) != null) {
            throw DomainException(
                errorCode = ErrorCode.VALIDATION_ERROR,
                status = 409,
                message = "user with email $normalizedEmail already exists"
            )
        }

        val now = clock.instant()
        val user = userRepository.save(
            User.create(
                displayName = normalizedDisplayName,
                email = normalizedEmail,
                passwordHash = passwordHasher.hash(normalizedPassword),
                now = now
            )
        )
        userActionTokenRepository.save(
            UserActionToken(
                userId = requireNotNull(user.id),
                tokenHash = hash("${UUID.randomUUID()}"),
                purpose = UserActionTokenPurpose.EMAIL_VERIFICATION,
                expiresAtUtc = now.plusSeconds(86400),
                createdAtUtc = now
            )
        )
        return issueTokens(user)
    }

    fun login(email: String, password: String): AuthResult {
        val normalizedEmail = requireValidEmail(email)
        val normalizedPassword = requireValidPassword(password)
        val user = userRepository.findByEmail(normalizedEmail)
            ?: throw unauthorized("email or password is invalid")
        if (user.status != UserStatus.ACTIVE) {
            throw unauthorized("account is not active")
        }
        if (!passwordHasher.matches(normalizedPassword, user.passwordHash)) {
            throw unauthorized("email or password is invalid")
        }
        return issueTokens(user)
    }

    fun refresh(refreshToken: String): AuthResult {
        val persistedRefreshToken = authTokenLifecycleService.rotate(refreshToken)
        val user = userRepository.findById(persistedRefreshToken.userId)
            ?: throw unauthorized("user does not exist")
        if (user.status != UserStatus.ACTIVE) {
            throw unauthorized("account is not active")
        }
        return issueTokens(user)
    }

    fun logout(userId: Long) {
        authTokenLifecycleService.revokeAll(userId)
    }

    private fun issueTokens(user: User): AuthResult {
        val accessToken = jwtTokenService.issueAccessToken(
            userId = requireNotNull(user.id),
            roles = setOf(ActorRole.USER),
            orgId = null
        )
        val refreshToken = authTokenLifecycleService.issueRefreshToken(requireNotNull(user.id))
        return AuthResult(
            accessToken = accessToken,
            refreshToken = refreshToken,
            user = user
        )
    }

    private fun hash(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
        return base64Encoder.encodeToString(digest)
    }

    private fun unauthorized(message: String): DomainException {
        return DomainException(
            errorCode = ErrorCode.UNAUTHORIZED,
            status = 401,
            message = message
        )
    }
}
