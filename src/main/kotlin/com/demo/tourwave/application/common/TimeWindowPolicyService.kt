package com.demo.tourwave.application.common

import com.demo.tourwave.domain.occurrence.Occurrence
import com.demo.tourwave.domain.participant.BookingParticipant
import com.demo.tourwave.domain.participant.BookingParticipantStatus
import java.time.Instant
import java.time.ZoneId

class TimeWindowPolicyService(
    private val invitationWindowMinutes: Long,
    private val invitationExpiryHours: Long,
    private val refundFullWindowHours: Long
) {
    fun isOfferExpired(now: Instant, offerExpiresAtUtc: Instant?): Boolean {
        return offerExpiresAtUtc?.let(now::isAfter) == true
    }

    fun invitationExpiresAt(participant: BookingParticipant, occurrence: Occurrence?): Instant? {
        val invitedAt = participant.invitedAt ?: return null
        val invitationLimit = invitedAt.plusSeconds(invitationExpiryHours * 60 * 60)
        val startsAtUtc = occurrence?.startsAtUtc ?: return invitationLimit
        return minOf(invitationLimit, startsAtUtc)
    }

    fun isInvitationExpired(participant: BookingParticipant, occurrence: Occurrence?, now: Instant): Boolean {
        if (participant.status != BookingParticipantStatus.INVITED) {
            return false
        }
        val expiresAt = invitationExpiresAt(participant, occurrence) ?: return false
        return !now.isBefore(expiresAt)
    }

    fun isInvitationWindowClosed(occurrence: Occurrence?, now: Instant): Boolean {
        val startsAtUtc = occurrence?.startsAtUtc ?: return false
        val zoneId = ZoneId.of(occurrence.timezone)
        val closeAt = startsAtUtc.atZone(zoneId).minusMinutes(invitationWindowMinutes).toInstant()
        return !now.isBefore(closeAt)
    }

    fun fullRefundDeadline(occurrence: Occurrence): Instant? {
        val startsAtUtc = occurrence.startsAtUtc ?: return null
        return startsAtUtc.atZone(ZoneId.of(occurrence.timezone)).minusHours(refundFullWindowHours).toInstant()
    }
}
