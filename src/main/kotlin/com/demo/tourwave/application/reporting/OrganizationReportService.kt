package com.demo.tourwave.application.reporting

import com.demo.tourwave.application.booking.port.BookingRepository
import com.demo.tourwave.application.booking.port.OccurrenceRepository
import com.demo.tourwave.application.booking.port.PaymentRecordRepository
import com.demo.tourwave.application.organization.OrganizationAccessGuard
import com.demo.tourwave.application.participant.port.BookingParticipantRepository
import com.demo.tourwave.application.tour.port.TourRepository
import com.demo.tourwave.domain.booking.AttendanceStatus
import com.demo.tourwave.domain.booking.BookingStatus
import com.demo.tourwave.domain.participant.BookingParticipantStatus
import com.demo.tourwave.domain.payment.PaymentRecordStatus
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.math.roundToInt

data class BookingReportQuery(
    val actorUserId: Long,
    val organizationId: Long,
    val dateFrom: LocalDate?,
    val dateTo: LocalDate?,
    val tourId: Long?,
    val occurrenceId: Long?,
    val cursor: String?,
    val limit: Int,
)

data class OccurrenceOpsReportQuery(
    val actorUserId: Long,
    val organizationId: Long,
    val dateFrom: LocalDate?,
    val dateTo: LocalDate?,
    val tourId: Long?,
    val occurrenceId: Long?,
    val cursor: String?,
    val limit: Int,
)

data class BookingReportItemView(
    val bookingId: Long,
    val occurrenceId: Long,
    val tourId: Long?,
    val organizerUserId: Long,
    val partySize: Int,
    val status: BookingStatus,
    val paymentStatus: com.demo.tourwave.domain.booking.PaymentStatus,
    val refundStatus: PaymentRecordStatus?,
    val createdAt: Instant,
)

data class OccurrenceOpsReportItemView(
    val occurrenceId: Long,
    val organizationId: Long,
    val tourId: Long?,
    val startsAtUtc: Instant?,
    val status: com.demo.tourwave.domain.occurrence.OccurrenceStatus,
    val capacity: Int,
    val confirmedSeats: Int,
    val waitlistCount: Int,
    val seatUtilizationPercent: Int,
    val attendedCount: Int,
    val noShowCount: Int,
    val refundedBookingCount: Int,
    val refundPendingCount: Int,
)

data class CursorPage<T>(
    val items: List<T>,
    val nextCursor: String?,
)

class OrganizationReportService(
    private val bookingRepository: BookingRepository,
    private val occurrenceRepository: OccurrenceRepository,
    private val bookingParticipantRepository: BookingParticipantRepository,
    private val paymentRecordRepository: PaymentRecordRepository,
    private val tourRepository: TourRepository,
    private val organizationAccessGuard: OrganizationAccessGuard,
) {
    fun getBookingReport(query: BookingReportQuery): CursorPage<BookingReportItemView> {
        organizationAccessGuard.requireOperator(query.actorUserId, query.organizationId)
        val safeLimit = query.limit.coerceIn(1, 100)
        val tourIds =
            query.tourId?.let { setOf(it) } ?: tourRepository.findByOrganizationId(query.organizationId).mapNotNull { it.id }.toSet()
        val occurrencesById =
            occurrenceRepository
                .findAll()
                .filter { it.organizationId == query.organizationId }
                .filter { query.occurrenceId == null || it.id == query.occurrenceId }
                .filter { it.tourId == null || it.tourId in tourIds }
                .associateBy { it.id }

        val filtered =
            bookingRepository
                .findAll()
                .filter { it.organizationId == query.organizationId }
                .filter { it.occurrenceId in occurrencesById.keys }
                .filter { booking ->
                    val date = booking.createdAt.atZone(ZoneOffset.UTC).toLocalDate()
                    (query.dateFrom == null || !date.isBefore(query.dateFrom)) &&
                        (query.dateTo == null || !date.isAfter(query.dateTo))
                }.sortedWith(
                    compareByDescending<com.demo.tourwave.domain.booking.Booking> { it.createdAt }.thenByDescending { it.id ?: -1L },
                )

        val paged = filtered.pageAfter(query.cursor, safeLimit) { requireNotNull(it.id) }
        return CursorPage(
            items =
                paged.items.map { booking ->
                    BookingReportItemView(
                        bookingId = requireNotNull(booking.id),
                        occurrenceId = booking.occurrenceId,
                        tourId = occurrencesById[booking.occurrenceId]?.tourId,
                        organizerUserId = booking.leaderUserId,
                        partySize = booking.partySize,
                        status = booking.status,
                        paymentStatus = booking.paymentStatus,
                        refundStatus = paymentRecordRepository.findByBookingId(requireNotNull(booking.id))?.status,
                        createdAt = booking.createdAt,
                    )
                },
            nextCursor = paged.nextCursor,
        )
    }

    fun exportBookingReportCsv(query: BookingReportQuery): String {
        val page = getBookingReport(query.copy(cursor = null, limit = Int.MAX_VALUE))
        val header = "bookingId,occurrenceId,tourId,organizerUserId,partySize,status,paymentStatus,refundStatus,createdAt"
        val lines =
            page.items.map {
                listOf(
                    it.bookingId.toString(),
                    it.occurrenceId.toString(),
                    it.tourId?.toString().orEmpty(),
                    it.organizerUserId.toString(),
                    it.partySize.toString(),
                    it.status.name,
                    it.paymentStatus.name,
                    it.refundStatus?.name.orEmpty(),
                    it.createdAt.toString(),
                ).joinToString(",")
            }
        return buildString {
            appendLine(header)
            lines.forEach { appendLine(it) }
        }
    }

    fun getOccurrenceOpsReport(query: OccurrenceOpsReportQuery): CursorPage<OccurrenceOpsReportItemView> {
        organizationAccessGuard.requireOperator(query.actorUserId, query.organizationId)
        val safeLimit = query.limit.coerceIn(1, 100)
        val tourIds =
            query.tourId?.let { setOf(it) } ?: tourRepository.findByOrganizationId(query.organizationId).mapNotNull { it.id }.toSet()
        val bookingsByOccurrence = bookingRepository.findAll().groupBy { it.occurrenceId }

        val filtered =
            occurrenceRepository
                .findAll()
                .asSequence()
                .filter { it.organizationId == query.organizationId }
                .filter { query.occurrenceId == null || it.id == query.occurrenceId }
                .filter { it.tourId == null || it.tourId in tourIds }
                .filter { occurrence ->
                    val date = occurrence.startsAtUtc?.atZone(ZoneOffset.UTC)?.toLocalDate()
                    (query.dateFrom == null || date == null || !date.isBefore(query.dateFrom)) &&
                        (query.dateTo == null || date == null || !date.isAfter(query.dateTo))
                }.sortedWith(
                    compareByDescending<com.demo.tourwave.domain.occurrence.Occurrence> {
                        it.startsAtUtc ?: Instant.EPOCH
                    }.thenByDescending { it.id },
                ).toList()

        val paged = filtered.pageAfter(query.cursor, safeLimit) { it.id }
        return CursorPage(
            items =
                paged.items.map { occurrence ->
                    val bookings = bookingsByOccurrence[occurrence.id].orEmpty()
                    val bookingIds = bookings.mapNotNull { it.id }
                    val participants = bookingIds.flatMap { bookingParticipantRepository.findByBookingId(it) }
                    val paymentRecords = bookingIds.mapNotNull(paymentRecordRepository::findByBookingId)
                    val confirmedSeats =
                        bookings
                            .filter { it.status == BookingStatus.CONFIRMED || it.status == BookingStatus.COMPLETED }
                            .sumOf { it.partySize }
                    val waitlistCount = bookings.count { it.status == BookingStatus.WAITLISTED }
                    val attendedCount =
                        participants.count {
                            (it.status == BookingParticipantStatus.LEADER || it.status == BookingParticipantStatus.ACCEPTED) &&
                                it.attendanceStatus == AttendanceStatus.ATTENDED
                        }
                    val noShowCount =
                        participants.count {
                            (it.status == BookingParticipantStatus.LEADER || it.status == BookingParticipantStatus.ACCEPTED) &&
                                it.attendanceStatus == AttendanceStatus.NO_SHOW
                        }
                    val refundedBookingCount = paymentRecords.count { it.status == PaymentRecordStatus.REFUNDED }
                    val refundPendingCount =
                        paymentRecords.count {
                            it.status == PaymentRecordStatus.REFUND_PENDING ||
                                it.status == PaymentRecordStatus.REFUND_FAILED_RETRYABLE ||
                                it.status == PaymentRecordStatus.REFUND_REVIEW_REQUIRED
                        }
                    OccurrenceOpsReportItemView(
                        occurrenceId = occurrence.id,
                        organizationId = occurrence.organizationId,
                        tourId = occurrence.tourId,
                        startsAtUtc = occurrence.startsAtUtc,
                        status = occurrence.status,
                        capacity = occurrence.capacity,
                        confirmedSeats = confirmedSeats,
                        waitlistCount = waitlistCount,
                        seatUtilizationPercent =
                            if (occurrence.capacity <=
                                0
                            ) {
                                0
                            } else {
                                ((confirmedSeats.toDouble() / occurrence.capacity.toDouble()) * 100).roundToInt()
                            },
                        attendedCount = attendedCount,
                        noShowCount = noShowCount,
                        refundedBookingCount = refundedBookingCount,
                        refundPendingCount = refundPendingCount,
                    )
                },
            nextCursor = paged.nextCursor,
        )
    }

    fun exportOccurrenceOpsReportCsv(query: OccurrenceOpsReportQuery): String {
        val page = getOccurrenceOpsReport(query.copy(cursor = null, limit = Int.MAX_VALUE))
        val header =
            "occurrenceId,organizationId,tourId,startsAtUtc,status,capacity,confirmedSeats," +
                "waitlistCount,seatUtilizationPercent,attendedCount,noShowCount,refundedBookingCount,refundPendingCount"
        val lines =
            page.items.map {
                listOf(
                    it.occurrenceId.toString(),
                    it.organizationId.toString(),
                    it.tourId?.toString().orEmpty(),
                    it.startsAtUtc?.toString().orEmpty(),
                    it.status.name,
                    it.capacity.toString(),
                    it.confirmedSeats.toString(),
                    it.waitlistCount.toString(),
                    it.seatUtilizationPercent.toString(),
                    it.attendedCount.toString(),
                    it.noShowCount.toString(),
                    it.refundedBookingCount.toString(),
                    it.refundPendingCount.toString(),
                ).joinToString(",")
            }
        return buildString {
            appendLine(header)
            lines.forEach { appendLine(it) }
        }
    }

    private data class PageSlice<T>(
        val items: List<T>,
        val nextCursor: String?,
    )

    private fun <T> List<T>.pageAfter(
        cursor: String?,
        limit: Int,
        idSelector: (T) -> Long,
    ): PageSlice<T> {
        val filtered =
            cursor?.toLongOrNull()?.let { cursorId ->
                dropWhile { idSelector(it) != cursorId }.drop(1)
            } ?: this
        val items = filtered.take(limit)
        val nextCursor =
            items
                .takeIf { filtered.size > limit }
                ?.lastOrNull()
                ?.let(idSelector)
                ?.toString()
        return PageSlice(items = items, nextCursor = nextCursor)
    }
}
