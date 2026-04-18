package com.demo.tourwave.adapter.out.persistence.jpa.review

import com.demo.tourwave.domain.review.ReviewType
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
    name = "reviews",
    uniqueConstraints = [UniqueConstraint(name = "uk_reviews_occurrence_reviewer_type", columnNames = ["occurrence_id", "reviewer_user_id", "type"])],
    indexes = [Index(name = "idx_reviews_occurrence_type", columnList = "occurrence_id,type")],
)
data class ReviewJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @Column(name = "occurrence_id", nullable = false)
    val occurrenceId: Long,
    @Column(name = "reviewer_user_id", nullable = false)
    val reviewerUserId: Long,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    val type: ReviewType,
    @Column(nullable = false)
    val rating: Int,
    @Column
    val comment: String? = null,
    @Column(name = "created_at", nullable = false)
    val createdAt: Instant,
)
