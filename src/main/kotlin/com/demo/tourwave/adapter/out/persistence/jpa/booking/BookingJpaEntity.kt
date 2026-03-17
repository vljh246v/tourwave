package com.demo.tourwave.adapter.out.persistence.jpa.booking

import com.demo.tourwave.domain.booking.BookingStatus
import com.demo.tourwave.domain.booking.PaymentStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(
    name = "bookings",
    indexes = [
        Index(name = "idx_bookings_occurrence", columnList = "occurrence_id"),
        Index(name = "idx_bookings_occurrence_status", columnList = "occurrence_id,status"),
        Index(name = "idx_bookings_status_offer", columnList = "status,offer_expires_at_utc")
    ]
)
data class BookingJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @Column(name = "occurrence_id", nullable = false)
    val occurrenceId: Long,
    @Column(name = "organization_id", nullable = false)
    val organizationId: Long,
    @Column(name = "leader_user_id", nullable = false)
    val leaderUserId: Long,
    @Column(name = "party_size", nullable = false)
    val partySize: Int,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    val status: BookingStatus,
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 32)
    val paymentStatus: PaymentStatus,
    @Column(name = "offer_expires_at_utc")
    val offerExpiresAtUtc: Instant? = null,
    @Column(name = "waitlist_skip_count", nullable = false)
    val waitlistSkipCount: Int = 0,
    @Column(name = "last_waitlist_action_at_utc")
    val lastWaitlistActionAtUtc: Instant? = null,
    @Column(name = "created_at", nullable = false)
    val createdAt: Instant
)
