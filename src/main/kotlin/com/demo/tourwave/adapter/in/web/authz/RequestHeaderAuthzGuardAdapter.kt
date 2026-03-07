package com.demo.tourwave.adapter.`in`.web.authz

import com.demo.tourwave.application.common.port.ActorAuthContext
import com.demo.tourwave.application.common.port.AuthzGuardPort
import com.demo.tourwave.domain.common.DomainException
import com.demo.tourwave.domain.common.ErrorCode
import org.springframework.stereotype.Component

@Component
class RequestHeaderAuthzGuardAdapter : AuthzGuardPort {
    override fun requireActorContext(
        actorUserId: Long?,
        actorOrgRole: String?,
        actorOrgId: Long?
    ): ActorAuthContext {
        val requiredActorUserId = actorUserId ?: throw DomainException(
            errorCode = ErrorCode.UNAUTHORIZED,
            status = 401,
            message = "X-Actor-User-Id is required",
            details = mapOf("header" to "X-Actor-User-Id")
        )

        val normalizedRole = actorOrgRole?.trim()?.ifEmpty { null }?.uppercase()

        if (normalizedRole == null && actorOrgId != null) {
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

        if (normalizedRole != null && actorOrgId == null) {
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

        if (normalizedRole != null && normalizedRole !in ALLOWED_ORG_ROLES) {
            throw DomainException(
                errorCode = ErrorCode.VALIDATION_ERROR,
                status = 422,
                message = "X-Actor-Org-Role is invalid",
                details = mapOf(
                    "header" to "X-Actor-Org-Role",
                    "allowedRoles" to ALLOWED_ORG_ROLES,
                    "providedRole" to normalizedRole
                )
            )
        }

        return ActorAuthContext(
            actorUserId = requiredActorUserId,
            actorOrgRole = normalizedRole,
            actorOrgId = actorOrgId
        )
    }

    private companion object {
        val ALLOWED_ORG_ROLES = setOf("ORG_MEMBER", "ORG_ADMIN", "ORG_OWNER")
    }
}
