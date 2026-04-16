package com.demo.tourwave.adapter.`in`.web.booking

import com.demo.tourwave.application.booking.ManualWaitlistActionCommand
import com.demo.tourwave.application.booking.ManualWaitlistActionResult
import com.demo.tourwave.application.booking.WaitlistOperatorService
import com.demo.tourwave.application.common.port.AuthzGuardPort
import com.demo.tourwave.domain.booking.Booking
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController

data class WaitlistOperatorActionWebRequest(
    val note: String? = null,
)

data class WaitlistOperatorActionWebResponse(
    val bookingId: Long,
    val status: String,
    val offerExpiresAtUtc: java.time.Instant?,
    val waitlistSkipCount: Int,
)

@RestController
class WaitlistOperatorController(
    private val waitlistOperatorService: WaitlistOperatorService,
    private val authzGuardPort: AuthzGuardPort,
) {
    @PostMapping("/bookings/{bookingId}/waitlist/promote")
    fun promote(
        @PathVariable bookingId: Long,
        @RequestHeader("X-Actor-User-Id", required = false) actorUserId: Long?,
        @RequestHeader("X-Actor-Org-Role", required = false) actorOrgRole: String?,
        @RequestHeader("X-Actor-Org-Id", required = false) actorOrgId: Long?,
        @RequestHeader("X-Request-Id", required = false) requestId: String?,
        @RequestBody(required = false) request: WaitlistOperatorActionWebRequest?,
    ): ResponseEntity<WaitlistOperatorActionWebResponse> {
        return execute(
            bookingId = bookingId,
            actorUserId = actorUserId,
            actorOrgRole = actorOrgRole,
            actorOrgId = actorOrgId,
            requestId = requestId,
            note = request?.note,
            action = waitlistOperatorService::promote,
        )
    }

    @PostMapping("/bookings/{bookingId}/waitlist/skip")
    fun skip(
        @PathVariable bookingId: Long,
        @RequestHeader("X-Actor-User-Id", required = false) actorUserId: Long?,
        @RequestHeader("X-Actor-Org-Role", required = false) actorOrgRole: String?,
        @RequestHeader("X-Actor-Org-Id", required = false) actorOrgId: Long?,
        @RequestHeader("X-Request-Id", required = false) requestId: String?,
        @RequestBody(required = false) request: WaitlistOperatorActionWebRequest?,
    ): ResponseEntity<WaitlistOperatorActionWebResponse> {
        return execute(
            bookingId = bookingId,
            actorUserId = actorUserId,
            actorOrgRole = actorOrgRole,
            actorOrgId = actorOrgId,
            requestId = requestId,
            note = request?.note,
            action = waitlistOperatorService::skip,
        )
    }

    private fun execute(
        bookingId: Long,
        actorUserId: Long?,
        actorOrgRole: String?,
        actorOrgId: Long?,
        requestId: String?,
        note: String?,
        action: (ManualWaitlistActionCommand) -> ManualWaitlistActionResult,
    ): ResponseEntity<WaitlistOperatorActionWebResponse> {
        val actor =
            authzGuardPort.requireActorContext(
                actorUserId = actorUserId,
                actorOrgRole = actorOrgRole,
                actorOrgId = actorOrgId,
            )
        val result =
            action(
                ManualWaitlistActionCommand(
                    bookingId = bookingId,
                    actor = actor,
                    note = note,
                    requestId = requestId,
                ),
            )
        return ResponseEntity.status(result.status).body(result.booking.toWebResponse())
    }

    private fun Booking.toWebResponse(): WaitlistOperatorActionWebResponse {
        return WaitlistOperatorActionWebResponse(
            bookingId = requireNotNull(id),
            status = status.name,
            offerExpiresAtUtc = offerExpiresAtUtc,
            waitlistSkipCount = waitlistSkipCount,
        )
    }
}
