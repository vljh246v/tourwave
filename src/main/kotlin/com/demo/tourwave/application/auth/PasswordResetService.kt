package com.demo.tourwave.application.auth

import com.demo.tourwave.application.common.port.AuditEventCommand
import com.demo.tourwave.application.common.port.AuditEventPort
import com.demo.tourwave.application.user.port.UserRepository
import com.demo.tourwave.domain.auth.UserActionTokenPurpose
import com.demo.tourwave.domain.common.DomainException
import com.demo.tourwave.domain.common.ErrorCode
import com.demo.tourwave.domain.user.UserStatus
import java.time.Clock
import java.time.Duration

class PasswordResetService(
    private val notificationChannelPort: NotificationChannelPort,
    private val userActionTokenService: UserActionTokenService,
    private val userRepository: UserRepository,
    private val auditEventPort: AuditEventPort,
    private val clock: Clock,
) {
    fun requestPasswordReset(email: String) {
        val normalizedEmail = requireValidEmail(email)
        val user = userRepository.findByEmail(normalizedEmail) ?: return
        if (user.status != UserStatus.ACTIVE) {
            return
        }

        val userId = requireNotNull(user.id)
        val rawToken =
            userActionTokenService.issue(
                userId = userId,
                purpose = UserActionTokenPurpose.PASSWORD_RESET,
                ttl = Duration.ofHours(2),
            )

        val expiresAt = clock.instant().plus(Duration.ofHours(2))
        val resetLink = "https://app.tourwave.com/auth/reset-password?token=$rawToken"
        val emailBody =
            """
            Password reset requested.
            
            Token: $rawToken
            Expires at: $expiresAt
            Reset link: $resetLink
            """.trimIndent()

        // Record audit event BEFORE email send (so it's recorded even if send fails)
        auditEventPort.append(
            AuditEventCommand(
                actor = "SYSTEM",
                action = "PASSWORD_RESET_EMAIL_SENT",
                resourceType = "USER",
                resourceId = userId,
                occurredAtUtc = clock.instant(),
            ),
        )

        try {
            notificationChannelPort.send(
                recipient = user.email,
                subject = "Password Reset Request",
                body = emailBody,
                templateType = "PASSWORD_RESET",
            )
        } catch (e: Exception) {
            throw DomainException(
                errorCode = ErrorCode.NOTIFICATION_SEND_FAILED,
                status = 500,
                message = "failed to send password reset email",
            )
        }
    }
}
