package com.demo.tourwave.adapter.`in`.web.customer

import com.demo.tourwave.application.common.port.AuthzGuardPort
import com.demo.tourwave.application.customer.CustomerBookingQueryService
import com.demo.tourwave.application.customer.FavoriteService
import com.demo.tourwave.application.customer.NotificationService
import com.demo.tourwave.domain.customer.Notification
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
class CustomerController(
    private val customerBookingQueryService: CustomerBookingQueryService,
    private val favoriteService: FavoriteService,
    private val notificationService: NotificationService,
    private val authzGuardPort: AuthzGuardPort
) {
    @GetMapping("/me/bookings")
    fun listMyBookings(
        @RequestHeader("X-Actor-User-Id", required = false) actorUserId: Long?
    ): ResponseEntity<List<MyBookingResponse>> {
        val requiredActorUserId = authzGuardPort.requireActorUserId(actorUserId)
        return ResponseEntity.ok(customerBookingQueryService.listMyBookings(requiredActorUserId).map { it.toResponse() })
    }

    @GetMapping("/bookings/{bookingId}/calendar.ics")
    fun bookingCalendar(
        @PathVariable bookingId: Long,
        @RequestHeader("X-Actor-User-Id", required = false) actorUserId: Long?,
        @RequestHeader("X-Actor-Org-Role", required = false) actorOrgRole: String?,
        @RequestHeader("X-Actor-Org-Id", required = false) actorOrgId: Long?
    ): ResponseEntity<String> {
        val actor = authzGuardPort.requireActorContext(
            actorUserId = actorUserId,
            actorOrgRole = actorOrgRole,
            actorOrgId = actorOrgId
        )
        val calendar = customerBookingQueryService.bookingCalendar(bookingId, actor)
        return calendarResponse(calendar.fileName, calendar.body)
    }

    @GetMapping("/occurrences/{occurrenceId}/calendar.ics")
    fun occurrenceCalendar(
        @PathVariable occurrenceId: Long
    ): ResponseEntity<String> {
        val calendar = customerBookingQueryService.occurrenceCalendar(occurrenceId)
        return calendarResponse(calendar.fileName, calendar.body)
    }

    @PostMapping("/tours/{tourId}/favorite")
    fun favoriteTour(
        @PathVariable tourId: Long,
        @RequestHeader("X-Actor-User-Id", required = false) actorUserId: Long?
    ): ResponseEntity<FavoriteResponse> {
        val requiredActorUserId = authzGuardPort.requireActorUserId(actorUserId)
        favoriteService.favorite(requiredActorUserId, tourId)
        return ResponseEntity.status(201).body(
            favoriteService.list(requiredActorUserId).first { it.tourId == tourId }.toResponse()
        )
    }

    @DeleteMapping("/tours/{tourId}/favorite")
    fun unfavoriteTour(
        @PathVariable tourId: Long,
        @RequestHeader("X-Actor-User-Id", required = false) actorUserId: Long?
    ): ResponseEntity<Void> {
        val requiredActorUserId = authzGuardPort.requireActorUserId(actorUserId)
        favoriteService.unfavorite(requiredActorUserId, tourId)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/me/favorites")
    fun listFavorites(
        @RequestHeader("X-Actor-User-Id", required = false) actorUserId: Long?
    ): ResponseEntity<List<FavoriteResponse>> {
        val requiredActorUserId = authzGuardPort.requireActorUserId(actorUserId)
        return ResponseEntity.ok(favoriteService.list(requiredActorUserId).map { it.toResponse() })
    }

    @GetMapping("/me/notifications")
    fun listNotifications(
        @RequestHeader("X-Actor-User-Id", required = false) actorUserId: Long?
    ): ResponseEntity<List<NotificationResponse>> {
        val requiredActorUserId = authzGuardPort.requireActorUserId(actorUserId)
        return ResponseEntity.ok(notificationService.list(requiredActorUserId).map { it.toResponse() })
    }

    @PostMapping("/me/notifications/{notificationId}/read")
    fun markNotificationRead(
        @PathVariable notificationId: Long,
        @RequestHeader("X-Actor-User-Id", required = false) actorUserId: Long?
    ): ResponseEntity<NotificationResponse> {
        val requiredActorUserId = authzGuardPort.requireActorUserId(actorUserId)
        return ResponseEntity.ok(notificationService.markRead(requiredActorUserId, notificationId).toResponse())
    }

    @PostMapping("/me/notifications/read-all")
    fun markAllNotificationsRead(
        @RequestHeader("X-Actor-User-Id", required = false) actorUserId: Long?
    ): ResponseEntity<List<NotificationResponse>> {
        val requiredActorUserId = authzGuardPort.requireActorUserId(actorUserId)
        return ResponseEntity.ok(notificationService.markAllRead(requiredActorUserId).map { it.toResponse() })
    }

    private fun calendarResponse(fileName: String, body: String): ResponseEntity<String> {
        return ResponseEntity.ok()
            .contentType(MediaType("text", "calendar"))
            .header(HttpHeaders.CONTENT_DISPOSITION, """attachment; filename="$fileName"""")
            .body(body)
    }
}

data class MyBookingResponse(
    val bookingId: Long,
    val occurrenceId: Long,
    val organizationId: Long,
    val organizationName: String?,
    val tourId: Long?,
    val tourTitle: String?,
    val partySize: Int,
    val status: String,
    val paymentStatus: String,
    val occurrenceStartsAtUtc: Instant?,
    val occurrenceEndsAtUtc: Instant?,
    val timezone: String?,
    val locationText: String?,
    val meetingPoint: String?,
    val createdAt: Instant
)

data class FavoriteResponse(
    val favoriteId: Long,
    val tourId: Long,
    val organizationId: Long,
    val title: String,
    val summary: String?,
    val attachmentAssetIds: List<Long>,
    val createdAt: Instant
)

data class NotificationResponse(
    val id: Long,
    val type: String,
    val title: String,
    val body: String,
    val resourceType: String,
    val resourceId: Long,
    val readAt: Instant?,
    val createdAt: Instant
)

private fun com.demo.tourwave.application.customer.MyBookingListItem.toResponse(): MyBookingResponse =
    MyBookingResponse(
        bookingId = bookingId,
        occurrenceId = occurrenceId,
        organizationId = organizationId,
        organizationName = organizationName,
        tourId = tourId,
        tourTitle = tourTitle,
        partySize = partySize,
        status = status.name,
        paymentStatus = paymentStatus.name,
        occurrenceStartsAtUtc = occurrenceStartsAtUtc,
        occurrenceEndsAtUtc = occurrenceEndsAtUtc,
        timezone = timezone,
        locationText = locationText,
        meetingPoint = meetingPoint,
        createdAt = createdAt
    )

private fun com.demo.tourwave.application.customer.FavoriteView.toResponse(): FavoriteResponse =
    FavoriteResponse(
        favoriteId = favoriteId,
        tourId = tourId,
        organizationId = organizationId,
        title = title,
        summary = summary,
        attachmentAssetIds = attachmentAssetIds,
        createdAt = createdAt
    )

private fun Notification.toResponse(): NotificationResponse =
    NotificationResponse(
        id = requireNotNull(id),
        type = type.name,
        title = title,
        body = body,
        resourceType = resourceType,
        resourceId = resourceId,
        readAt = readAt,
        createdAt = createdAt
    )
