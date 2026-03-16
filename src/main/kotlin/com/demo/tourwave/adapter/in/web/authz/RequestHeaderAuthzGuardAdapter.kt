package com.demo.tourwave.adapter.`in`.web.authz

import com.demo.tourwave.application.common.port.ActorAuthContext
import com.demo.tourwave.application.common.port.AuthzGuardPort
import org.springframework.stereotype.Component

@Component
class RequestHeaderAuthzGuardAdapter(
    private val requestHeaderActorContextResolver: RequestHeaderActorContextResolver
) : AuthzGuardPort {
    override fun requireActorContext(
        actorUserId: Long?,
        actorRole: String?,
        actorOrgRole: String?,
        actorOrgId: Long?,
        requestId: String?
    ): ActorAuthContext {
        return requestHeaderActorContextResolver.resolve(
            actorUserId = actorUserId,
            actorRole = actorRole,
            actorOrgRole = actorOrgRole,
            actorOrgId = actorOrgId,
            requestId = requestId
        )
    }
}
