package com.demo.tourwave.adapter.`in`.web.participant

import com.demo.tourwave.application.common.port.AuthzGuardPort
import com.demo.tourwave.application.participant.GetOccurrenceRosterQuery
import com.demo.tourwave.application.participant.OccurrenceRosterEntryView
import com.demo.tourwave.application.participant.OccurrenceRosterResult
import com.demo.tourwave.application.participant.ParticipantRosterQueryService
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

data class OccurrenceRosterWebResponse(
    val occurrenceId: Long,
    val organizationId: Long,
    val tourId: Long?,
    val instructorProfileId: Long?,
    val items: List<OccurrenceRosterEntryWebResponse>,
)

data class OccurrenceRosterEntryWebResponse(
    val occurrenceId: Long,
    val bookingId: Long,
    val organizationId: Long,
    val bookingLeaderUserId: Long,
    val bookingStatus: String,
    val participantId: Long,
    val participantUserId: Long,
    val participantStatus: String,
    val attendanceStatus: String,
)

@RestController
class ParticipantRosterController(
    private val participantRosterQueryService: ParticipantRosterQueryService,
    private val authzGuardPort: AuthzGuardPort,
) {
    @GetMapping("/occurrences/{occurrenceId}/participants/roster")
    fun getRoster(
        @PathVariable occurrenceId: Long,
        @RequestHeader("X-Actor-User-Id", required = false) actorUserId: Long?,
        @RequestHeader("X-Actor-Org-Role", required = false) actorOrgRole: String?,
        @RequestHeader("X-Actor-Org-Id", required = false) actorOrgId: Long?,
    ): ResponseEntity<OccurrenceRosterWebResponse> {
        val result = query(occurrenceId, actorUserId, actorOrgRole, actorOrgId)
        return ResponseEntity.ok(result.toWebResponse())
    }

    @GetMapping("/occurrences/{occurrenceId}/participants/roster/export", produces = ["text/csv"])
    fun exportRoster(
        @PathVariable occurrenceId: Long,
        @RequestHeader("X-Actor-User-Id", required = false) actorUserId: Long?,
        @RequestHeader("X-Actor-Org-Role", required = false) actorOrgRole: String?,
        @RequestHeader("X-Actor-Org-Id", required = false) actorOrgId: Long?,
        @RequestParam(required = false, defaultValue = "csv") format: String,
    ): ResponseEntity<String> {
        if (format.lowercase() != "csv") {
            throw IllegalArgumentException("Only csv export is supported")
        }
        val result = query(occurrenceId, actorUserId, actorOrgRole, actorOrgId)
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("text/csv"))
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"occurrence-$occurrenceId-roster.csv\"")
            .body(result.toCsv())
    }

    private fun query(
        occurrenceId: Long,
        actorUserId: Long?,
        actorOrgRole: String?,
        actorOrgId: Long?,
    ): OccurrenceRosterResult {
        val actor =
            authzGuardPort.requireActorContext(
                actorUserId = actorUserId,
                actorOrgRole = actorOrgRole,
                actorOrgId = actorOrgId,
            )
        return participantRosterQueryService.getOccurrenceRoster(
            GetOccurrenceRosterQuery(
                occurrenceId = occurrenceId,
                actor = actor,
            ),
        )
    }

    private fun OccurrenceRosterResult.toWebResponse(): OccurrenceRosterWebResponse {
        return OccurrenceRosterWebResponse(
            occurrenceId = occurrenceId,
            organizationId = organizationId,
            tourId = tourId,
            instructorProfileId = instructorProfileId,
            items = items.map { it.toWebResponse() },
        )
    }

    private fun OccurrenceRosterEntryView.toWebResponse(): OccurrenceRosterEntryWebResponse {
        return OccurrenceRosterEntryWebResponse(
            occurrenceId = occurrenceId,
            bookingId = bookingId,
            organizationId = organizationId,
            bookingLeaderUserId = bookingLeaderUserId,
            bookingStatus = bookingStatus.name,
            participantId = participantId,
            participantUserId = participantUserId,
            participantStatus = participantStatus.name,
            attendanceStatus = attendanceStatus.name,
        )
    }

    private fun OccurrenceRosterResult.toCsv(): String {
        val header =
            "occurrenceId,organizationId,tourId,instructorProfileId," +
                "bookingId,bookingLeaderUserId,bookingStatus,participantId,participantUserId,participantStatus,attendanceStatus"
        val rows =
            items.joinToString("\n") { entry ->
                listOf(
                    entry.occurrenceId,
                    organizationId,
                    tourId ?: "",
                    instructorProfileId ?: "",
                    entry.bookingId,
                    entry.bookingLeaderUserId,
                    entry.bookingStatus.name,
                    entry.participantId,
                    entry.participantUserId,
                    entry.participantStatus.name,
                    entry.attendanceStatus.name,
                ).joinToString(",")
            }
        return if (rows.isEmpty()) header else "$header\n$rows"
    }
}
