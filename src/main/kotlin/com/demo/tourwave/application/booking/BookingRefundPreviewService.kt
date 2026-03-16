package com.demo.tourwave.application.booking

import com.demo.tourwave.application.booking.port.BookingRepository
import com.demo.tourwave.application.booking.port.OccurrenceRepository
import com.demo.tourwave.application.common.port.ActorAuthContext
import com.demo.tourwave.application.participant.ParticipantAccessPolicy
import com.demo.tourwave.application.participant.ParticipantAccessType
import com.demo.tourwave.domain.booking.BookingStatus
import com.demo.tourwave.domain.booking.PaymentStatus
import com.demo.tourwave.domain.booking.RefundDecisionType
import com.demo.tourwave.domain.booking.RefundPolicyAction
import com.demo.tourwave.domain.booking.RefundReasonCode
import com.demo.tourwave.domain.common.DomainException
import com.demo.tourwave.domain.common.ErrorCode
import java.time.Clock
import java.time.Instant

data class GetBookingRefundPreviewQuery(
    val bookingId: Long,
    val actor: ActorAuthContext
)

data class BookingRefundPreviewView(
    val bookingId: Long,
    val cancelable: Boolean,
    val status: BookingStatus,
    val paymentStatus: PaymentStatus,
    val refundDecisionType: RefundDecisionType?,
    val refundReasonCode: RefundReasonCode?,
    val refundable: Boolean,
    val occurrenceStartsAtUtc: Instant?,
    val evaluatedAtUtc: Instant
)

class BookingRefundPreviewService(
    private val bookingRepository: BookingRepository,
    private val occurrenceRepository: OccurrenceRepository,
    private val participantAccessPolicy: ParticipantAccessPolicy,
    private val paymentLedgerService: PaymentLedgerService,
    private val clock: Clock
) {
    fun getPreview(query: GetBookingRefundPreviewQuery): BookingRefundPreviewView {
        val accessType = participantAccessPolicy.authorizeBookingParticipants(
            bookingId = query.bookingId,
            actor = query.actor
        )

        val booking = bookingRepository.findById(query.bookingId)
            ?: error("Booking access policy must guarantee booking existence")

        if (accessType == ParticipantAccessType.BOOKING_PARTICIPANT && booking.leaderUserId != query.actor.actorUserId) {
            throw DomainException(
                errorCode = ErrorCode.FORBIDDEN,
                status = 403,
                message = "Only booking leader or org operator can preview cancellation refund",
                details = mapOf("bookingId" to query.bookingId, "actorUserId" to query.actor.actorUserId)
            )
        }

        val occurrence = occurrenceRepository.getOrCreate(booking.occurrenceId)
        val cancelable = !booking.status.isTerminal()

        if (!cancelable) {
            return BookingRefundPreviewView(
                bookingId = requireNotNull(booking.id),
                cancelable = false,
                status = booking.status,
                paymentStatus = booking.paymentStatus,
                refundDecisionType = null,
                refundReasonCode = null,
                refundable = false,
                occurrenceStartsAtUtc = occurrence.startsAtUtc,
                evaluatedAtUtc = clock.instant()
            )
        }

        val decision = paymentLedgerService.decisionFor(
            booking = booking,
            occurrence = occurrence,
            action = RefundPolicyAction.LEADER_CANCEL
        )

        return BookingRefundPreviewView(
            bookingId = requireNotNull(booking.id),
            cancelable = true,
            status = booking.status,
            paymentStatus = booking.paymentStatus,
            refundDecisionType = decision.type,
            refundReasonCode = decision.reasonCode,
            refundable = decision.refundable,
            occurrenceStartsAtUtc = occurrence.startsAtUtc,
            evaluatedAtUtc = clock.instant()
        )
    }
}
