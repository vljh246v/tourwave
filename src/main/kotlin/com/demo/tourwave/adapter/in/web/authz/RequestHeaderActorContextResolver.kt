package com.demo.tourwave.adapter.`in`.web.authz

import com.demo.tourwave.application.common.port.ActorAuthContext
import com.demo.tourwave.application.common.port.ActorRole
import com.demo.tourwave.domain.common.DomainException
import com.demo.tourwave.domain.common.ErrorCode
import org.springframework.stereotype.Component

@Component
class RequestHeaderActorContextResolver {
    fun resolve(
        actorUserId: Long?,
        actorRole: String?,
        actorOrgRole: String?,
        actorOrgId: Long?,
        requestId: String?
    ): ActorAuthContext {
        val requiredActorUserId = actorUserId ?: throw DomainException(
            errorCode = ErrorCode.UNAUTHORIZED,
            status = 401,
            message = "X-Actor-User-Id is required",
            details = mapOf("header" to "X-Actor-User-Id")
        )

        val resolvedActorRole = ActorRole.parseActorRole(actorRole)
        if (actorRole != null && resolvedActorRole == null) {
            throw DomainException(
                errorCode = ErrorCode.VALIDATION_ERROR,
                status = 422,
                message = "X-Actor-Role is invalid",
                details = mapOf(
                    "header" to "X-Actor-Role",
                    "allowedRoles" to listOf(ActorRole.USER.name, ActorRole.INSTRUCTOR.name),
                    "providedRole" to actorRole.trim().uppercase()
                )
            )
        }

        val resolvedOrgRole = ActorRole.parseOrgRole(actorOrgRole)
        if (actorOrgRole != null && resolvedOrgRole == null) {
            throw DomainException(
                errorCode = ErrorCode.VALIDATION_ERROR,
                status = 422,
                message = "X-Actor-Org-Role is invalid",
                details = mapOf(
                    "header" to "X-Actor-Org-Role",
                    "allowedRoles" to ActorRole.ORG_ROLES.map { it.name },
                    "providedRole" to actorOrgRole.trim().uppercase()
                )
            )
        }

        if (resolvedOrgRole == null && actorOrgId != null) {
            throw DomainException(
                errorCode = ErrorCode.REQUIRED_FIELD_MISSING,
                status = 422,
                message = "X-Actor-Org-Role is required when X-Actor-Org-Id is provided",
                details = mapOf(
                    "field" to "X-Actor-Org-Role",
                    "header" to "X-Actor-Org-Role"
                )
            )
        }

        if (resolvedOrgRole != null && actorOrgId == null) {
            throw DomainException(
                errorCode = ErrorCode.REQUIRED_FIELD_MISSING,
                status = 422,
                message = "X-Actor-Org-Id is required when X-Actor-Org-Role is provided",
                details = mapOf(
                    "field" to "X-Actor-Org-Id",
                    "header" to "X-Actor-Org-Id"
                )
            )
        }

        if (actorOrgId != null && actorOrgId <= 0L) {
            throw DomainException(
                errorCode = ErrorCode.VALIDATION_ERROR,
                status = 422,
                message = "X-Actor-Org-Id must be positive",
                details = mapOf("header" to "X-Actor-Org-Id")
            )
        }

        val roles = linkedSetOf<ActorRole>()
        roles += resolvedActorRole ?: ActorRole.USER
        if (resolvedOrgRole != null) {
            roles += resolvedOrgRole
        }

        return ActorAuthContext(
            actorUserId = requiredActorUserId,
            roles = roles,
            actorOrgId = actorOrgId,
            requestId = requestId
        )
    }
}
