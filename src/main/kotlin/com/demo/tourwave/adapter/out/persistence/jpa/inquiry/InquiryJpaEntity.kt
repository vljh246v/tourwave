package com.demo.tourwave.adapter.out.persistence.jpa.inquiry

import com.demo.tourwave.domain.inquiry.InquiryStatus
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
    name = "inquiries",
    uniqueConstraints = [UniqueConstraint(name = "uk_inquiries_booking", columnNames = ["booking_id"])],
    indexes = [
        Index(name = "idx_inquiries_booking", columnList = "booking_id"),
        Index(name = "idx_inquiries_creator_created", columnList = "created_by_user_id,created_at"),
    ],
)
data class InquiryJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @Column(name = "organization_id", nullable = false)
    val organizationId: Long,
    @Column(name = "occurrence_id", nullable = false)
    val occurrenceId: Long,
    @Column(name = "booking_id", nullable = false)
    val bookingId: Long,
    @Column(name = "created_by_user_id", nullable = false)
    val createdByUserId: Long,
    @Column
    val subject: String? = null,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    val status: InquiryStatus = InquiryStatus.OPEN,
    @Column(name = "created_at", nullable = false)
    val createdAt: Instant,
)
