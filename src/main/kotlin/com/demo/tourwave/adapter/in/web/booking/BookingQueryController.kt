package com.demo.tourwave.adapter.`in`.web.booking

import com.demo.tourwave.application.booking.BookingDetailParticipantView
import com.demo.tourwave.application.booking.BookingDetailView
import com.demo.tourwave.application.booking.BookingOccurrenceView
import com.demo.tourwave.application.booking.BookingQueryService
import com.demo.tourwave.application.booking.GetBookingDetailQuery
import com.demo.tourwave.application.common.port.AuthzGuardPort
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController

@RestController
class BookingQueryController(
    private val bookingQueryService: BookingQueryService,
    private val authzGuardPort: AuthzGuardPort
) {
    @GetMapping("/bookings/{bookingId}")
    fun getBookingDetail(
        @PathVariable bookingId: Long,
        @RequestHeader("X-Actor-User-Id", required = false) actorUserId: Long?,
        @RequestHeader("X-Actor-Org-Role", required = false) actorOrgRole: String?,
        @RequestHeader("X-Actor-Org-Id", required = false) actorOrgId: Long?
    ): ResponseEntity<BookingDetailWebResponse> {
        val actor = authzGuardPort.requireActorContext(
            actorUserId = actorUserId,
            actorOrgRole = actorOrgRole,
            actorOrgId = actorOrgId
        )

        val result = bookingQueryService.getBookingDetail(
            GetBookingDetailQuery(
                bookingId = bookingId,
                actor = actor
            )
        )

        return ResponseEntity.ok(result.toWebResponse())
    }

    private fun BookingDetailView.toWebResponse(): BookingDetailWebResponse {
        return BookingDetailWebResponse(
            id = id,
            organizationId = organizationId,
            occurrenceId = occurrenceId,
            userId = userId,
            partySize = partySize,
            status = status,
            paymentStatus = paymentStatus,
            createdAt = createdAt,
            offerExpiresAtUtc = offerExpiresAtUtc,
            occurrence = occurrence.toWebResponse(),
            participants = participants.map { it.toWebResponse() }
        )
    }

    private fun BookingOccurrenceView.toWebResponse(): BookingOccurrenceWebResponse {
        return BookingOccurrenceWebResponse(
            id = id,
            organizationId = organizationId,
            tourId = tourId,
            instructorProfileId = instructorProfileId,
            capacity = capacity,
            startsAtUtc = startsAtUtc,
            status = status
        )
    }

    private fun BookingDetailParticipantView.toWebResponse(): BookingDetailParticipantWebResponse {
        return BookingDetailParticipantWebResponse(
            id = id,
            bookingId = bookingId,
            userId = userId,
            status = status,
            attendanceStatus = attendanceStatus,
            invitedAt = invitedAt,
            respondedAt = respondedAt
        )
    }
}
