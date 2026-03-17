package com.demo.tourwave.adapter.`in`.web.authz

import com.demo.tourwave.application.auth.JwtTokenService
import com.demo.tourwave.application.common.port.ActorAuthContext
import com.demo.tourwave.application.common.port.AuthzGuardPort
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

@Component
class RequestHeaderAuthzGuardAdapter(
    private val requestHeaderActorContextResolver: RequestHeaderActorContextResolver,
    private val jwtTokenService: JwtTokenService,
    @Value("\${tourwave.auth.allow-header-auth-fallback:true}")
    private val allowHeaderAuthFallback: Boolean
) : AuthzGuardPort {
    override fun requireActorContext(
        actorUserId: Long?,
        actorRole: String?,
        actorOrgRole: String?,
        actorOrgId: Long?,
        requestId: String?
    ): ActorAuthContext {
        resolveJwtContext(requestId)?.let { return it }
        if (!allowHeaderAuthFallback) {
            return requestHeaderActorContextResolver.resolve(
                actorUserId = null,
                actorRole = null,
                actorOrgRole = null,
                actorOrgId = null,
                requestId = requestId
            )
        }
        return requestHeaderActorContextResolver.resolve(
            actorUserId = actorUserId,
            actorRole = actorRole,
            actorOrgRole = actorOrgRole,
            actorOrgId = actorOrgId,
            requestId = requestId
        )
    }

    private fun resolveJwtContext(requestId: String?): ActorAuthContext? {
        val servletRequestAttributes = RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes ?: return null
        val header = servletRequestAttributes.request.getHeader("Authorization") ?: return null
        if (!header.startsWith("Bearer ")) {
            return null
        }
        val claims = jwtTokenService.parse(header.removePrefix("Bearer ").trim())
        return ActorAuthContext(
            actorUserId = claims.userId,
            roles = claims.roles,
            actorOrgId = claims.orgId,
            requestId = requestId
        )
    }
}
