package com.demo.tourwave.adapter.`in`.job

import com.demo.tourwave.application.participant.InvitationExpirationJobResult
import com.demo.tourwave.application.participant.InvitedParticipantExpirationService
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
    private val invitedParticipantExpirationService: InvitedParticipantExpirationService
) {
    @Scheduled(fixedDelayString = "\${tourwave.jobs.invitation-expiration.fixed-delay-ms:60000}")
    fun run(): InvitationExpirationJobResult {
        return invitedParticipantExpirationService.expireInvitations()
    }
}
