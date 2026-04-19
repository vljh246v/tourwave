package com.demo.tourwave.application.user

import com.demo.tourwave.application.common.port.AuditEventCommand
import com.demo.tourwave.application.common.port.AuditEventPort
import com.demo.tourwave.domain.common.DomainException
import com.demo.tourwave.domain.common.ErrorCode
import com.demo.tourwave.domain.user.User
import java.time.Clock

class UserService(
    private val userPort: UserPort,
    private val auditEventPort: AuditEventPort,
    private val clock: Clock,
) {
    fun getCurrentUser(userId: Long): User {
        return userPort.findById(userId) ?: throw DomainException(
            errorCode = ErrorCode.UNAUTHORIZED,
            status = 401,
            message = "authenticated user does not exist",
        )
    }

    fun updateProfile(
        userId: Long,
        displayName: String,
    ): User {
        val normalizedDisplayName = requireValidDisplayName(displayName)
        val user = getCurrentUser(userId)
        val updated = user.updateProfile(displayName = normalizedDisplayName, now = clock.instant())
        val saved = userPort.save(updated)
        auditEventPort.append(
            AuditEventCommand(
                actor = "USER:$userId",
                action = "USER_PROFILE_UPDATED",
                resourceType = "USER",
                resourceId = userId,
                occurredAtUtc = clock.instant(),
                reasonCode = "SELF_SERVICE_PROFILE_UPDATE",
                afterJson = mapOf("displayName" to saved.displayName),
            ),
        )
        return saved
    }

    fun getOrCreate(
        email: String,
        displayName: String,
    ): User {
        val normalizedEmail = email.trim().lowercase()
        val existing = userPort.findByEmail(normalizedEmail)
        if (existing != null) return existing
        val now = clock.instant()
        return userPort.save(
            User.create(
                displayName = displayName,
                email = normalizedEmail,
                passwordHash = "[OAUTH]",
                now = now,
            ),
        )
    }

    private fun requireValidDisplayName(displayName: String): String {
        val normalized = displayName.trim()
        if (normalized.isBlank() || normalized.length > 100) {
            throw DomainException(
                errorCode = ErrorCode.VALIDATION_ERROR,
                status = 422,
                message = "displayName must be between 1 and 100 characters",
            )
        }
        return normalized
    }
}
