package com.demo.tourwave.adapter.`in`.web.announcement

import com.demo.tourwave.application.announcement.AnnouncementService
import com.demo.tourwave.application.announcement.CreateAnnouncementCommand
import com.demo.tourwave.application.announcement.UpdateAnnouncementCommand
import com.demo.tourwave.application.common.port.AuthzGuardPort
import com.demo.tourwave.domain.announcement.Announcement
import com.demo.tourwave.domain.announcement.AnnouncementVisibility
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
class AnnouncementController(
    private val announcementService: AnnouncementService,
    private val authzGuardPort: AuthzGuardPort,
) {
    @GetMapping("/public/announcements")
    fun listPublicAnnouncements(
        @RequestParam(required = false) organizationId: Long?,
        @RequestParam(required = false) cursor: String?,
        @RequestParam(required = false, defaultValue = "20") limit: Int,
    ): ResponseEntity<AnnouncementListResponse> {
        val page = announcementService.listPublicAnnouncements(organizationId, cursor, limit)
        return ResponseEntity.ok(AnnouncementListResponse(page.items.map { it.toResponse() }, page.nextCursor))
    }

    @GetMapping("/operator/organizations/{organizationId}/announcements")
    fun listOperatorAnnouncements(
        @PathVariable organizationId: Long,
        @RequestHeader("X-Actor-User-Id", required = false) actorUserId: Long?,
        @RequestParam(required = false) cursor: String?,
        @RequestParam(required = false, defaultValue = "20") limit: Int,
    ): ResponseEntity<AnnouncementListResponse> {
        val page =
            announcementService.listOperatorAnnouncements(
                actorUserId = authzGuardPort.requireActorUserId(actorUserId),
                organizationId = organizationId,
                cursor = cursor,
                limit = limit,
            )
        return ResponseEntity.ok(AnnouncementListResponse(page.items.map { it.toResponse() }, page.nextCursor))
    }

    @PostMapping("/organizations/{organizationId}/announcements")
    fun createAnnouncement(
        @PathVariable organizationId: Long,
        @RequestHeader("X-Actor-User-Id", required = false) actorUserId: Long?,
        @RequestBody request: AnnouncementCreateRequest,
    ): ResponseEntity<AnnouncementResponse> {
        val announcement =
            announcementService.create(
                CreateAnnouncementCommand(
                    actorUserId = authzGuardPort.requireActorUserId(actorUserId),
                    organizationId = organizationId,
                    title = request.title,
                    body = request.body,
                    visibility = request.visibility,
                    publishStartsAtUtc = request.publishStartsAtUtc,
                    publishEndsAtUtc = request.publishEndsAtUtc,
                ),
            )
        return ResponseEntity.status(201).body(announcement.toResponse())
    }

    @PatchMapping("/announcements/{announcementId}")
    fun updateAnnouncement(
        @PathVariable announcementId: Long,
        @RequestHeader("X-Actor-User-Id", required = false) actorUserId: Long?,
        @RequestBody request: AnnouncementPatchRequest,
    ): ResponseEntity<AnnouncementResponse> {
        val announcement =
            announcementService.update(
                UpdateAnnouncementCommand(
                    actorUserId = authzGuardPort.requireActorUserId(actorUserId),
                    announcementId = announcementId,
                    title = request.title,
                    body = request.body,
                    visibility = request.visibility,
                    publishStartsAtUtc = request.publishStartsAtUtc,
                    publishEndsAtUtc = request.publishEndsAtUtc,
                ),
            )
        return ResponseEntity.ok(announcement.toResponse())
    }

    @DeleteMapping("/announcements/{announcementId}")
    fun deleteAnnouncement(
        @PathVariable announcementId: Long,
        @RequestHeader("X-Actor-User-Id", required = false) actorUserId: Long?,
    ): ResponseEntity<Void> {
        announcementService.delete(authzGuardPort.requireActorUserId(actorUserId), announcementId)
        return ResponseEntity.noContent().build()
    }
}

data class AnnouncementCreateRequest(
    val title: String,
    val body: String,
    val visibility: AnnouncementVisibility = AnnouncementVisibility.DRAFT,
    val publishStartsAtUtc: Instant? = null,
    val publishEndsAtUtc: Instant? = null,
)

data class AnnouncementPatchRequest(
    val title: String? = null,
    val body: String? = null,
    val visibility: AnnouncementVisibility? = null,
    val publishStartsAtUtc: Instant? = null,
    val publishEndsAtUtc: Instant? = null,
)

data class AnnouncementResponse(
    val id: Long,
    val organizationId: Long,
    val title: String,
    val body: String,
    val visibility: AnnouncementVisibility,
    val publishStartsAtUtc: Instant?,
    val publishEndsAtUtc: Instant?,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class AnnouncementListResponse(
    val items: List<AnnouncementResponse>,
    val nextCursor: String?,
)

private fun Announcement.toResponse(): AnnouncementResponse =
    AnnouncementResponse(
        id = requireNotNull(id),
        organizationId = organizationId,
        title = title,
        body = body,
        visibility = visibility,
        publishStartsAtUtc = publishStartsAtUtc,
        publishEndsAtUtc = publishEndsAtUtc,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
