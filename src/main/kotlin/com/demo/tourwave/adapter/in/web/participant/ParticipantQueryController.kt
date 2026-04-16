package com.demo.tourwave.adapter.`in`.web.participant

import com.demo.tourwave.application.common.port.AuthzGuardPort
import com.demo.tourwave.application.participant.BookingParticipantListResult
import com.demo.tourwave.application.participant.ListBookingParticipantsQuery
import com.demo.tourwave.application.participant.ParticipantQueryService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController

@RestController
class ParticipantQueryController(
    private val participantQueryService: ParticipantQueryService,
    private val authzGuardPort: AuthzGuardPort,
) {
    @GetMapping("/bookings/{bookingId}/participants")
    fun listBookingParticipants(
        @PathVariable bookingId: Long,
        @RequestHeader("X-Actor-User-Id", required = false) actorUserId: Long?,
        @RequestHeader("X-Actor-Org-Role", required = false) actorOrgRole: String?,
        @RequestHeader("X-Actor-Org-Id", required = false) actorOrgId: Long?,
    ): ResponseEntity<ParticipantListWebResponse> {
        val actor =
            authzGuardPort.requireActorContext(
                actorUserId = actorUserId,
                actorOrgRole = actorOrgRole,
                actorOrgId = actorOrgId,
            )

        val result =
            participantQueryService.listBookingParticipants(
                ListBookingParticipantsQuery(
                    bookingId = bookingId,
                    actor = actor,
                ),
            )

        return ResponseEntity.ok(result.toWebResponse())
    }

    private fun BookingParticipantListResult.toWebResponse(): ParticipantListWebResponse {
        return ParticipantListWebResponse(
            items =
                items.map {
                    ParticipantDetailWebResponse(
                        id = it.id,
                        bookingId = it.bookingId,
                        userId = it.userId,
                        status = it.status,
                        attendanceStatus = it.attendanceStatus,
                        invitedAt = it.invitedAt,
                        respondedAt = it.respondedAt,
                    )
                },
        )
    }
}
