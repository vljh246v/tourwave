package com.demo.tourwave.adapter.`in`.web.occurrence

import com.demo.tourwave.application.common.port.AuthzGuardPort
import com.demo.tourwave.application.occurrence.CreateOccurrenceCommand
import com.demo.tourwave.application.occurrence.OccurrenceCommandService
import com.demo.tourwave.application.occurrence.RescheduleOccurrenceCommand
import com.demo.tourwave.application.occurrence.UpdateOccurrenceCommand
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController

@RestController
class OccurrenceOperatorController(
    private val occurrenceCommandService: OccurrenceCommandService,
    private val authzGuardPort: AuthzGuardPort,
) {
    @PostMapping("/tours/{tourId}/occurrences")
    fun createOccurrence(
        @PathVariable tourId: Long,
        @RequestHeader("X-Actor-User-Id", required = false) actorUserId: Long?,
        @RequestBody request: CreateOccurrenceWebRequest,
    ): ResponseEntity<OccurrenceResponse> {
        val requiredActorUserId = authzGuardPort.requireActorUserId(actorUserId)
        val occurrence =
            occurrenceCommandService.create(
                CreateOccurrenceCommand(
                    actorUserId = requiredActorUserId,
                    tourId = tourId,
                    instructorProfileId = request.instructorProfileId,
                    capacity = request.capacity,
                    startsAtUtc = request.startsAtUtc,
                    endsAtUtc = request.endsAtUtc,
                    timezone = request.timezone,
                    unitPrice = request.unitPrice,
                    currency = request.currency,
                    locationText = request.locationText,
                    meetingPoint = request.meetingPoint,
                ),
            )
        return ResponseEntity.status(201).body(occurrence.toPublicView().toResponse())
    }

    @PatchMapping("/occurrences/{occurrenceId}")
    fun updateOccurrence(
        @PathVariable occurrenceId: Long,
        @RequestHeader("X-Actor-User-Id", required = false) actorUserId: Long?,
        @RequestBody request: UpdateOccurrenceWebRequest,
    ): ResponseEntity<OccurrenceResponse> {
        val requiredActorUserId = authzGuardPort.requireActorUserId(actorUserId)
        val occurrence =
            occurrenceCommandService.update(
                UpdateOccurrenceCommand(
                    actorUserId = requiredActorUserId,
                    occurrenceId = occurrenceId,
                    instructorProfileId = request.instructorProfileId,
                    capacity = request.capacity,
                    startsAtUtc = request.startsAtUtc,
                    endsAtUtc = request.endsAtUtc,
                    timezone = request.timezone,
                    locationText = request.locationText,
                    meetingPoint = request.meetingPoint,
                ),
            )
        return ResponseEntity.ok(occurrence.toPublicView().toResponse())
    }

    @PostMapping("/occurrences/{occurrenceId}/reschedule")
    fun rescheduleOccurrence(
        @PathVariable occurrenceId: Long,
        @RequestHeader("X-Actor-User-Id", required = false) actorUserId: Long?,
        @RequestBody request: RescheduleOccurrenceWebRequest,
    ): ResponseEntity<OccurrenceResponse> {
        val requiredActorUserId = authzGuardPort.requireActorUserId(actorUserId)
        val occurrence =
            occurrenceCommandService.reschedule(
                RescheduleOccurrenceCommand(
                    actorUserId = requiredActorUserId,
                    occurrenceId = occurrenceId,
                    startsAtUtc = request.startsAtUtc,
                    endsAtUtc = request.endsAtUtc,
                    timezone = request.timezone,
                    locationText = request.locationText,
                    meetingPoint = request.meetingPoint,
                ),
            )
        return ResponseEntity.ok(occurrence.toPublicView().toResponse())
    }
}
