package com.demo.tourwave.domain.participant

import com.demo.tourwave.domain.booking.AttendanceStatus
import java.time.Instant

data class BookingParticipant(
    val id: Long? = null,
    val bookingId: Long,
    val userId: Long,
    val status: BookingParticipantStatus,
    val attendanceStatus: AttendanceStatus = AttendanceStatus.UNKNOWN,
    val invitedAt: Instant? = null,
    val respondedAt: Instant? = null,
    val createdAt: Instant = Instant.now()
) {
    companion object {
        fun leader(
            bookingId: Long,
            userId: Long,
            createdAt: Instant
        ): BookingParticipant {
            return BookingParticipant(
                bookingId = bookingId,
                userId = userId,
                status = BookingParticipantStatus.LEADER,
                createdAt = createdAt
            )
        }
    }

    fun isActive(): Boolean {
        return status == BookingParticipantStatus.LEADER ||
            status == BookingParticipantStatus.INVITED ||
            status == BookingParticipantStatus.ACCEPTED
    }

    fun cancel(canceledAt: Instant): BookingParticipant {
        if (!isActive()) {
            return this
        }
        return copy(status = BookingParticipantStatus.CANCELED, respondedAt = canceledAt)
    }

    fun accept(respondedAt: Instant): BookingParticipant {
        require(status == BookingParticipantStatus.INVITED) { "Only invited participant can accept" }
        return copy(status = BookingParticipantStatus.ACCEPTED, respondedAt = respondedAt)
    }

    fun decline(respondedAt: Instant): BookingParticipant {
        require(status == BookingParticipantStatus.INVITED) { "Only invited participant can decline" }
        return copy(status = BookingParticipantStatus.DECLINED, respondedAt = respondedAt)
    }

    fun expire(expiredAt: Instant): BookingParticipant {
        if (status != BookingParticipantStatus.INVITED) {
            return this
        }
        return copy(status = BookingParticipantStatus.EXPIRED, respondedAt = expiredAt)
    }

    fun recordAttendance(attendanceStatus: AttendanceStatus): BookingParticipant {
        require(attendanceStatus != AttendanceStatus.UNKNOWN) { "attendanceStatus must be explicit" }
        require(status == BookingParticipantStatus.LEADER || status == BookingParticipantStatus.ACCEPTED) {
            "Attendance can be recorded only for attending participant"
        }
        return copy(attendanceStatus = attendanceStatus)
    }
}
