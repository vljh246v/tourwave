package com.demo.tourwave.application.common.port

data class ActorAuthContext(
    val actorUserId: Long,
    val actorOrgRole: String? = null,
    val actorOrgId: Long? = null
)

interface AuthzGuardPort {
    fun requireActorContext(
        actorUserId: Long?,
        actorOrgRole: String? = null,
        actorOrgId: Long? = null
    ): ActorAuthContext

    fun requireActorUserId(actorUserId: Long?): Long {
        return requireActorContext(actorUserId = actorUserId).actorUserId
    }
}
