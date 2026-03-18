package com.demo.tourwave.adapter.`in`.job

import com.demo.tourwave.application.participant.InvitationExpirationJobResult
import com.demo.tourwave.application.participant.InvitedParticipantExpirationService
import com.demo.tourwave.application.common.ScheduledJobCoordinator
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(
    prefix = "tourwave.jobs.invitation-expiration",
    name = ["enabled"],
    havingValue = "true"
)
class InvitationExpirationJob(
    private val invitedParticipantExpirationService: InvitedParticipantExpirationService,
    private val scheduledJobCoordinator: ScheduledJobCoordinator
) {
    @Scheduled(fixedDelayString = "\${tourwave.jobs.invitation-expiration.fixed-delay-ms:60000}")
    fun run(): InvitationExpirationJobResult {
        return scheduledJobCoordinator.run(
            jobName = "invitation-expiration",
            onSkipped = { InvitationExpirationJobResult(expiredParticipantIds = emptyList()) }
        ) {
            invitedParticipantExpirationService.expireInvitations()
        }
    }
}
