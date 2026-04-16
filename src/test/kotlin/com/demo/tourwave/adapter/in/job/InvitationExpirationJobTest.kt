package com.demo.tourwave.adapter.`in`.job

import com.demo.tourwave.application.common.ScheduledJobCoordinator
import com.demo.tourwave.application.participant.InvitationExpirationJobResult
import com.demo.tourwave.application.participant.InvitedParticipantExpirationService
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals

class InvitationExpirationJobTest {
    private val invitationExpirationService = mock<InvitedParticipantExpirationService>()
    private val scheduledJobCoordinator = mock<ScheduledJobCoordinator>()
    private val job = InvitationExpirationJob(invitationExpirationService, scheduledJobCoordinator)

    @Test
    fun `job delegates to invitation expiration service`() {
        whenever(scheduledJobCoordinator.run(eq("invitation-expiration"), any(), any<() -> InvitationExpirationJobResult>()))
            .thenAnswer { invocation -> invocation.getArgument<() -> InvitationExpirationJobResult>(2).invoke() }
        whenever(invitationExpirationService.expireInvitations()).thenReturn(
            InvitationExpirationJobResult(expiredParticipantIds = listOf(3L)),
        )

        val result = job.run()

        verify(invitationExpirationService).expireInvitations()
        assertEquals(listOf(3L), result.expiredParticipantIds)
    }
}
