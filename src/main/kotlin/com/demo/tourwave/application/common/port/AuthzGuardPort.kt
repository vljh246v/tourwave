package com.demo.tourwave.application.common.port

enum class ActorRole {
    USER,
    INSTRUCTOR,
    ORG_MEMBER,
    ORG_ADMIN,
    ORG_OWNER,
    ;

    companion object {
        val ORG_ROLES = setOf(ORG_MEMBER, ORG_ADMIN, ORG_OWNER)

        fun parseActorRole(raw: String?): ActorRole? {
            val normalized = raw?.trim()?.ifEmpty { null }?.uppercase() ?: return null
            return when (normalized) {
                USER.name -> USER
                INSTRUCTOR.name -> INSTRUCTOR
                else -> null
            }
        }

        fun parseOrgRole(raw: String?): ActorRole? {
            val normalized = raw?.trim()?.ifEmpty { null }?.uppercase() ?: return null
            return when (normalized) {
                ORG_MEMBER.name -> ORG_MEMBER
                ORG_ADMIN.name -> ORG_ADMIN
                ORG_OWNER.name -> ORG_OWNER
                else -> null
            }
        }
    }
}

data class ActorAuthContext(
    val actorUserId: Long,
    val roles: Set<ActorRole> = setOf(ActorRole.USER),
    val actorOrgId: Long? = null,
    val requestId: String? = null,
) {
    fun hasRole(role: ActorRole): Boolean = role in roles

    fun hasAnyRole(vararg expected: ActorRole): Boolean {
        return expected.any { it in roles }
    }

    fun isOrgOperator(): Boolean {
        return hasAnyRole(ActorRole.ORG_ADMIN, ActorRole.ORG_OWNER)
    }
}

interface AuthzGuardPort {
    fun requireActorContext(
        actorUserId: Long?,
        actorRole: String? = null,
        actorOrgRole: String? = null,
        actorOrgId: Long? = null,
        requestId: String? = null,
    ): ActorAuthContext

    fun requireActorUserId(actorUserId: Long?): Long {
        return requireActorContext(actorUserId = actorUserId).actorUserId
    }
}
