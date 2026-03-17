package com.demo.tourwave.adapter.out.persistence.jpa.participant

import com.demo.tourwave.domain.booking.AttendanceStatus
import com.demo.tourwave.domain.participant.BookingParticipantStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant

@Entity
@Table(
    name = "booking_participants",
    uniqueConstraints = [UniqueConstraint(name = "uk_booking_participants_booking_user", columnNames = ["booking_id", "user_id"])],
    indexes = [
        Index(name = "idx_booking_participants_booking", columnList = "booking_id"),
        Index(name = "idx_booking_participants_status", columnList = "status")
    ]
)
data class BookingParticipantJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @Column(name = "booking_id", nullable = false)
    val bookingId: Long,
    @Column(name = "user_id", nullable = false)
    val userId: Long,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    val status: BookingParticipantStatus,
    @Enumerated(EnumType.STRING)
    @Column(name = "attendance_status", nullable = false, length = 32)
    val attendanceStatus: AttendanceStatus = AttendanceStatus.UNKNOWN,
    @Column(name = "invited_at")
    val invitedAt: Instant? = null,
    @Column(name = "responded_at")
    val respondedAt: Instant? = null,
    @Column(name = "created_at", nullable = false)
    val createdAt: Instant
)
