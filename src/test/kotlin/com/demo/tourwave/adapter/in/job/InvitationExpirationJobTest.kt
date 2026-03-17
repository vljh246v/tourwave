package com.demo.tourwave.adapter.`in`.job

import com.demo.tourwave.application.participant.InvitationExpirationJobResult
import com.demo.tourwave.application.participant.InvitedParticipantExpirationService
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals

class InvitationExpirationJobTest {
    private val invitationExpirationService = mock<InvitedParticipantExpirationService>()
    private val job = InvitationExpirationJob(invitationExpirationService)

    @Test
    fun `job delegates to invitation expiration service`() {
        whenever(invitationExpirationService.expireInvitations()).thenReturn(
            InvitationExpirationJobResult(expiredParticipantIds = listOf(3L))
        )

        val result = job.run()

        verify(invitationExpirationService).expireInvitations()
        assertEquals(listOf(3L), result.expiredParticipantIds)
    }
}
