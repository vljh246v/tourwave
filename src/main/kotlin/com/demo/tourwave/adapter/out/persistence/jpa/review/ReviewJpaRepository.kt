package com.demo.tourwave.adapter.out.persistence.jpa.review

import com.demo.tourwave.domain.review.ReviewType
import org.springframework.data.jpa.repository.JpaRepository

interface ReviewJpaRepository : JpaRepository<ReviewJpaEntity, Long> {
    fun findByOccurrenceIdAndReviewerUserIdAndType(
        occurrenceId: Long,
        reviewerUserId: Long,
        type: ReviewType,
    ): ReviewJpaEntity?

    fun findByOccurrenceIdAndTypeOrderByCreatedAtAsc(
        occurrenceId: Long,
        type: ReviewType,
    ): List<ReviewJpaEntity>

    fun findAllByOrderByIdAsc(): List<ReviewJpaEntity>
}
