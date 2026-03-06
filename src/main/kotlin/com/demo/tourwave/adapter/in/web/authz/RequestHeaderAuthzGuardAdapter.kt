package com.demo.tourwave.adapter.`in`.web.authz

import com.demo.tourwave.application.common.port.AuthzGuardPort
import com.demo.tourwave.domain.common.DomainException
import com.demo.tourwave.domain.common.ErrorCode
import org.springframework.stereotype.Component

@Component
class RequestHeaderAuthzGuardAdapter : AuthzGuardPort {
    override fun requireActorUserId(actorUserId: Long?): Long {
        return actorUserId ?: throw DomainException(
            errorCode = ErrorCode.UNAUTHORIZED,
            status = 401,
            message = "X-Actor-User-Id is required",
            details = mapOf("header" to "X-Actor-User-Id")
        )
    }
}

