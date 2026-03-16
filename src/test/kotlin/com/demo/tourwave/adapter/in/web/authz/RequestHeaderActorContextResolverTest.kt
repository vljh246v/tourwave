package com.demo.tourwave.adapter.`in`.web.authz

import com.demo.tourwave.application.common.port.ActorRole
import com.demo.tourwave.domain.common.DomainException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class RequestHeaderActorContextResolverTest {
    private val resolver = RequestHeaderActorContextResolver()

    @Test
    fun `resolve returns typed roles for instructor org admin actor`() {
        val actor = resolver.resolve(
            actorUserId = 42L,
            actorRole = "instructor",
            actorOrgRole = "org_admin",
            actorOrgId = 31L,
            requestId = "req-1"
        )

        assertEquals(42L, actor.actorUserId)
        assertEquals(31L, actor.actorOrgId)
        assertEquals("req-1", actor.requestId)
        assertTrue(actor.hasRole(ActorRole.INSTRUCTOR))
        assertTrue(actor.hasRole(ActorRole.ORG_ADMIN))
    }

    @Test
    fun `resolve rejects org id without org role`() {
        val exception = assertThrows<DomainException> {
            resolver.resolve(
                actorUserId = 42L,
                actorRole = null,
                actorOrgRole = null,
                actorOrgId = 31L,
                requestId = null
            )
        }

        assertEquals(422, exception.status)
    }

    @Test
    fun `resolve rejects unknown actor role`() {
        val exception = assertThrows<DomainException> {
            resolver.resolve(
                actorUserId = 42L,
                actorRole = "manager",
                actorOrgRole = null,
                actorOrgId = null,
                requestId = null
            )
        }

        assertEquals(422, exception.status)
    }
}
