package com.demo.tourwave.adapter.`in`.web.reporting

import com.demo.tourwave.application.common.port.AuthzGuardPort
import com.demo.tourwave.application.reporting.BookingReportItemView
import com.demo.tourwave.application.reporting.BookingReportQuery
import com.demo.tourwave.application.reporting.OccurrenceOpsReportItemView
import com.demo.tourwave.application.reporting.OccurrenceOpsReportQuery
import com.demo.tourwave.application.reporting.OrganizationReportService
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import java.time.LocalDate

@RestController
class OrganizationReportController(
    private val organizationReportService: OrganizationReportService,
    private val authzGuardPort: AuthzGuardPort
) {
    @GetMapping("/organizations/{organizationId}/reports/bookings")
    fun getBookingReport(
        @PathVariable organizationId: Long,
        @RequestHeader("X-Actor-User-Id", required = false) actorUserId: Long?,
        @RequestParam(required = false) dateFrom: LocalDate?,
        @RequestParam(required = false) dateTo: LocalDate?,
        @RequestParam(required = false) tourId: Long?,
        @RequestParam(required = false) occurrenceId: Long?,
        @RequestParam(required = false) cursor: String?,
        @RequestParam(required = false, defaultValue = "20") limit: Int
    ): ResponseEntity<BookingReportResponse> {
        val page = organizationReportService.getBookingReport(
            BookingReportQuery(
                actorUserId = authzGuardPort.requireActorUserId(actorUserId),
                organizationId = organizationId,
                dateFrom = dateFrom,
                dateTo = dateTo,
                tourId = tourId,
                occurrenceId = occurrenceId,
                cursor = cursor,
                limit = limit
            )
        )
        return ResponseEntity.ok(BookingReportResponse(page.items.map { it.toResponse() }, page.nextCursor))
    }

    @GetMapping("/organizations/{organizationId}/reports/bookings/export", produces = ["text/csv"])
    fun exportBookingReport(
        @PathVariable organizationId: Long,
        @RequestHeader("X-Actor-User-Id", required = false) actorUserId: Long?,
        @RequestParam(required = false) dateFrom: LocalDate?,
        @RequestParam(required = false) dateTo: LocalDate?,
        @RequestParam(required = false) tourId: Long?,
        @RequestParam(required = false) occurrenceId: Long?
    ): ResponseEntity<String> {
        val csv = organizationReportService.exportBookingReportCsv(
            BookingReportQuery(
                actorUserId = authzGuardPort.requireActorUserId(actorUserId),
                organizationId = organizationId,
                dateFrom = dateFrom,
                dateTo = dateTo,
                tourId = tourId,
                occurrenceId = occurrenceId,
                cursor = null,
                limit = Int.MAX_VALUE
            )
        )
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("text/csv"))
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"organization-$organizationId-bookings.csv\"")
            .body(csv)
    }

    @GetMapping("/organizations/{organizationId}/reports/occurrences")
    fun getOccurrenceOpsReport(
        @PathVariable organizationId: Long,
        @RequestHeader("X-Actor-User-Id", required = false) actorUserId: Long?,
        @RequestParam(required = false) dateFrom: LocalDate?,
        @RequestParam(required = false) dateTo: LocalDate?,
        @RequestParam(required = false) tourId: Long?,
        @RequestParam(required = false) occurrenceId: Long?,
        @RequestParam(required = false) cursor: String?,
        @RequestParam(required = false, defaultValue = "20") limit: Int
    ): ResponseEntity<OccurrenceOpsReportResponse> {
        val page = organizationReportService.getOccurrenceOpsReport(
            OccurrenceOpsReportQuery(
                actorUserId = authzGuardPort.requireActorUserId(actorUserId),
                organizationId = organizationId,
                dateFrom = dateFrom,
                dateTo = dateTo,
                tourId = tourId,
                occurrenceId = occurrenceId,
                cursor = cursor,
                limit = limit
            )
        )
        return ResponseEntity.ok(OccurrenceOpsReportResponse(page.items.map { it.toResponse() }, page.nextCursor))
    }

    @GetMapping("/organizations/{organizationId}/reports/occurrences/export", produces = ["text/csv"])
    fun exportOccurrenceOpsReport(
        @PathVariable organizationId: Long,
        @RequestHeader("X-Actor-User-Id", required = false) actorUserId: Long?,
        @RequestParam(required = false) dateFrom: LocalDate?,
        @RequestParam(required = false) dateTo: LocalDate?,
        @RequestParam(required = false) tourId: Long?,
        @RequestParam(required = false) occurrenceId: Long?
    ): ResponseEntity<String> {
        val csv = organizationReportService.exportOccurrenceOpsReportCsv(
            OccurrenceOpsReportQuery(
                actorUserId = authzGuardPort.requireActorUserId(actorUserId),
                organizationId = organizationId,
                dateFrom = dateFrom,
                dateTo = dateTo,
                tourId = tourId,
                occurrenceId = occurrenceId,
                cursor = null,
                limit = Int.MAX_VALUE
            )
        )
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("text/csv"))
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"organization-$organizationId-occurrences.csv\"")
            .body(csv)
    }
}

data class BookingReportResponse(
    val items: List<BookingReportItemResponse>,
    val nextCursor: String?
)

data class BookingReportItemResponse(
    val bookingId: Long,
    val occurrenceId: Long,
    val tourId: Long?,
    val organizerUserId: Long,
    val partySize: Int,
    val status: String,
    val paymentStatus: String,
    val refundStatus: String?,
    val createdAt: Instant
)

data class OccurrenceOpsReportResponse(
    val items: List<OccurrenceOpsReportItemResponse>,
    val nextCursor: String?
)

data class OccurrenceOpsReportItemResponse(
    val occurrenceId: Long,
    val organizationId: Long,
    val tourId: Long?,
    val startsAtUtc: Instant?,
    val status: String,
    val capacity: Int,
    val confirmedSeats: Int,
    val waitlistCount: Int,
    val seatUtilizationPercent: Int,
    val attendedCount: Int,
    val noShowCount: Int,
    val refundedBookingCount: Int,
    val refundPendingCount: Int
)

private fun BookingReportItemView.toResponse(): BookingReportItemResponse =
    BookingReportItemResponse(
        bookingId = bookingId,
        occurrenceId = occurrenceId,
        tourId = tourId,
        organizerUserId = organizerUserId,
        partySize = partySize,
        status = status.name,
        paymentStatus = paymentStatus.name,
        refundStatus = refundStatus?.name,
        createdAt = createdAt
    )

private fun OccurrenceOpsReportItemView.toResponse(): OccurrenceOpsReportItemResponse =
    OccurrenceOpsReportItemResponse(
        occurrenceId = occurrenceId,
        organizationId = organizationId,
        tourId = tourId,
        startsAtUtc = startsAtUtc,
        status = status.name,
        capacity = capacity,
        confirmedSeats = confirmedSeats,
        waitlistCount = waitlistCount,
        seatUtilizationPercent = seatUtilizationPercent,
        attendedCount = attendedCount,
        noShowCount = noShowCount,
        refundedBookingCount = refundedBookingCount,
        refundPendingCount = refundPendingCount
    )
