package com.demo.tourwave.application.auth

import com.demo.tourwave.application.auth.port.PasswordHasher
import com.demo.tourwave.application.common.port.ActorRole
import com.demo.tourwave.application.common.port.AuditEventCommand
import com.demo.tourwave.application.common.port.AuditEventPort
import com.demo.tourwave.application.user.port.UserRepository
import com.demo.tourwave.domain.auth.UserActionTokenPurpose
import com.demo.tourwave.domain.common.DomainException
import com.demo.tourwave.domain.common.ErrorCode
import com.demo.tourwave.domain.user.User
import com.demo.tourwave.domain.user.UserStatus
import java.time.Clock
import java.time.Duration

class AuthCommandService(
    private val userRepository: UserRepository,
    private val passwordHasher: PasswordHasher,
    private val jwtTokenService: JwtTokenService,
    private val authTokenLifecycleService: AuthTokenLifecycleService,
    private val userActionTokenService: UserActionTokenService,
    private val auditEventPort: AuditEventPort,
    private val clock: Clock,
) {
    fun signup(
        displayName: String,
        email: String,
        password: String,
    ): AuthResult {
        val normalizedEmail = requireValidEmail(email)
        val normalizedDisplayName = requireValidDisplayName(displayName)
        val normalizedPassword = requireValidPassword(password)
        if (userRepository.findByEmail(normalizedEmail) != null) {
            throw DomainException(
                errorCode = ErrorCode.VALIDATION_ERROR,
                status = 409,
                message = "user with email $normalizedEmail already exists",
            )
        }

        val now = clock.instant()
        val user =
            userRepository.save(
                User.create(
                    displayName = normalizedDisplayName,
                    email = normalizedEmail,
                    passwordHash = passwordHasher.hash(normalizedPassword),
                    now = now,
                ),
            )
        userActionTokenService.issue(
            userId = requireNotNull(user.id),
            purpose = UserActionTokenPurpose.EMAIL_VERIFICATION,
            ttl = Duration.ofHours(24),
        )
        return issueTokens(user)
    }

    fun login(
        email: String,
        password: String,
    ): AuthResult {
        val normalizedEmail = requireValidEmail(email)
        val normalizedPassword = requireValidPassword(password)
        val user =
            userRepository.findByEmail(normalizedEmail)
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
        val user =
            userRepository.findById(persistedRefreshToken.userId)
                ?: throw unauthorized("user does not exist")
        if (user.status != UserStatus.ACTIVE) {
            throw unauthorized("account is not active")
        }
        return issueTokens(user)
    }

    fun logout(userId: Long) {
        authTokenLifecycleService.revokeAll(userId)
    }

    fun requestEmailVerification(userId: Long) {
        val user =
            userRepository.findById(userId)
                ?: throw unauthorized("authenticated user does not exist")
        if (user.status != UserStatus.ACTIVE || user.emailVerifiedAt != null) {
            return
        }
        userActionTokenService.issue(
            userId = userId,
            purpose = UserActionTokenPurpose.EMAIL_VERIFICATION,
            ttl = Duration.ofHours(24),
        )
    }

    fun confirmEmailVerification(token: String) {
        val actionToken = userActionTokenService.consume(token, UserActionTokenPurpose.EMAIL_VERIFICATION)
        val user =
            userRepository.findById(actionToken.userId)
                ?: throw invalidTokenOwner()
        val verified = user.verifyEmail(clock.instant())
        userRepository.save(verified)
        auditEventPort.append(
            AuditEventCommand(
                actor = "USER:${actionToken.userId}",
                action = "USER_EMAIL_VERIFIED",
                resourceType = "USER",
                resourceId = actionToken.userId,
                occurredAtUtc = clock.instant(),
                reasonCode = if (user.emailVerifiedAt == null) "EMAIL_VERIFIED" else "EMAIL_ALREADY_VERIFIED",
                afterJson = mapOf("emailVerifiedAt" to verified.emailVerifiedAt?.toString()),
            ),
        )
    }

    fun requestPasswordReset(email: String) {
        val normalizedEmail = requireValidEmail(email)
        val user = userRepository.findByEmail(normalizedEmail) ?: return
        if (user.status != UserStatus.ACTIVE) {
            return
        }
        userActionTokenService.issue(
            userId = requireNotNull(user.id),
            purpose = UserActionTokenPurpose.PASSWORD_RESET,
            ttl = Duration.ofHours(2),
        )
    }

    fun confirmPasswordReset(
        token: String,
        newPassword: String,
    ) {
        val normalizedPassword = requireValidPassword(newPassword)
        val actionToken = userActionTokenService.consume(token, UserActionTokenPurpose.PASSWORD_RESET)
        val user =
            userRepository.findById(actionToken.userId)
                ?: throw invalidTokenOwner()
        if (user.status != UserStatus.ACTIVE) {
            throw unauthorized("account is not active")
        }
        userRepository.save(
            user.updatePassword(
                passwordHash = passwordHasher.hash(normalizedPassword),
                now = clock.instant(),
            ),
        )
        authTokenLifecycleService.revokeAll(actionToken.userId)
        auditEventPort.append(
            AuditEventCommand(
                actor = "SYSTEM",
                action = "USER_PASSWORD_RESET",
                resourceType = "USER",
                resourceId = actionToken.userId,
                occurredAtUtc = clock.instant(),
                reasonCode = "PASSWORD_RESET_CONFIRMED",
            ),
        )
    }

    fun deactivate(userId: Long) {
        val user =
            userRepository.findById(userId)
                ?: throw unauthorized("authenticated user does not exist")
        if (user.status == UserStatus.DEACTIVATED) {
            authTokenLifecycleService.revokeAll(userId)
            return
        }
        val deactivated = user.deactivate(clock.instant())
        userRepository.save(deactivated)
        authTokenLifecycleService.revokeAll(userId)
        auditEventPort.append(
            AuditEventCommand(
                actor = "USER:$userId",
                action = "USER_DEACTIVATED",
                resourceType = "USER",
                resourceId = userId,
                occurredAtUtc = clock.instant(),
                reasonCode = "SELF_SERVICE_DEACTIVATION",
                afterJson = mapOf("status" to deactivated.status.name),
            ),
        )
    }

    private fun issueTokens(user: User): AuthResult {
        val accessToken =
            jwtTokenService.issueAccessToken(
                userId = requireNotNull(user.id),
                roles = setOf(ActorRole.USER),
                orgId = null,
            )
        val refreshToken = authTokenLifecycleService.issueRefreshToken(requireNotNull(user.id))
        return AuthResult(
            accessToken = accessToken,
            refreshToken = refreshToken,
            user = user,
        )
    }

    private fun unauthorized(message: String): DomainException {
        return DomainException(
            errorCode = ErrorCode.UNAUTHORIZED,
            status = 401,
            message = message,
        )
    }

    private fun invalidTokenOwner(): DomainException {
        return DomainException(
            errorCode = ErrorCode.VALIDATION_ERROR,
            status = 400,
            message = "action token owner is invalid",
        )
    }
}
