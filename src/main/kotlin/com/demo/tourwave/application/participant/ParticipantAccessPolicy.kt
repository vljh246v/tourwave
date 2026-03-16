package com.demo.tourwave.application.participant

import com.demo.tourwave.application.booking.port.BookingRepository
import com.demo.tourwave.application.common.port.ActorAuthContext
import com.demo.tourwave.application.participant.port.BookingParticipantRepository
import com.demo.tourwave.domain.common.DomainException
import com.demo.tourwave.domain.common.ErrorCode
import com.demo.tourwave.domain.participant.BookingParticipantStatus

enum class ParticipantAccessType {
    BOOKING_PARTICIPANT,
    ORG_OPERATOR
}

class ParticipantAccessPolicy(
    private val bookingRepository: BookingRepository,
    private val bookingParticipantRepository: BookingParticipantRepository
) {
    fun authorizeBookingParticipants(bookingId: Long, actor: ActorAuthContext): ParticipantAccessType {
        val booking = bookingRepository.findById(bookingId)
            ?: throw DomainException(
                errorCode = ErrorCode.VALIDATION_ERROR,
                status = 404,
                message = "Booking not found",
                details = mapOf("bookingId" to bookingId)
            )

        val participant = bookingParticipantRepository.findByBookingIdAndUserId(bookingId, actor.actorUserId)
        if (participant != null && (participant.status == BookingParticipantStatus.LEADER || participant.status == BookingParticipantStatus.ACCEPTED)) {
            return ParticipantAccessType.BOOKING_PARTICIPANT
        }

        if (actor.isOrgOperator()) {
            val actorOrgId = actor.actorOrgId
                ?: throw DomainException(
                    errorCode = ErrorCode.REQUIRED_FIELD_MISSING,
                    status = 422,
                    message = "X-Actor-Org-Id is required for org operator access",
                    details = mapOf("field" to "X-Actor-Org-Id")
                )

            if (actorOrgId != booking.organizationId) {
                throw DomainException(
                    errorCode = ErrorCode.FORBIDDEN,
                    status = 403,
                    message = "operator organization does not match booking scope",
                    details = mapOf(
                        "bookingId" to bookingId,
                        "bookingOrganizationId" to booking.organizationId,
                        "actorOrganizationId" to actorOrgId
                    )
                )
            }

            return ParticipantAccessType.ORG_OPERATOR
        }

        throw DomainException(
            errorCode = ErrorCode.FORBIDDEN,
            status = 403,
            message = "Forbidden participant access",
            details = mapOf("bookingId" to bookingId, "actorUserId" to actor.actorUserId)
        )
    }
}
