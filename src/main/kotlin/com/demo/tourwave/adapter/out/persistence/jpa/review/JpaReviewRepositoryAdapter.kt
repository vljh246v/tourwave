package com.demo.tourwave.adapter.out.persistence.jpa.review

import com.demo.tourwave.application.review.port.ReviewRepository
import com.demo.tourwave.domain.review.Review
import com.demo.tourwave.domain.review.ReviewType
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Repository

@Repository
@Profile("mysql", "mysql-test")
class JpaReviewRepositoryAdapter(
    private val reviewJpaRepository: ReviewJpaRepository,
) : ReviewRepository {
    override fun save(review: Review): Review = reviewJpaRepository.save(review.toEntity()).toDomain()

    override fun findByOccurrenceAndReviewerAndType(
        occurrenceId: Long,
        reviewerUserId: Long,
        type: ReviewType,
    ): Review? = reviewJpaRepository.findByOccurrenceIdAndReviewerUserIdAndType(occurrenceId, reviewerUserId, type)?.toDomain()

    override fun findByOccurrenceAndType(
        occurrenceId: Long,
        type: ReviewType,
    ): List<Review> = reviewJpaRepository.findByOccurrenceIdAndTypeOrderByCreatedAtAsc(occurrenceId, type).map { it.toDomain() }

    override fun findAll(): List<Review> = reviewJpaRepository.findAllByOrderByIdAsc().map { it.toDomain() }

    override fun clear() {
        reviewJpaRepository.deleteAllInBatch()
    }
}

private fun Review.toEntity(): ReviewJpaEntity =
    ReviewJpaEntity(
        id = id,
        occurrenceId = occurrenceId,
        reviewerUserId = reviewerUserId,
        type = type,
        rating = rating,
        comment = comment,
        createdAt = createdAt,
    )

private fun ReviewJpaEntity.toDomain(): Review =
    Review(
        id = id,
        occurrenceId = occurrenceId,
        reviewerUserId = reviewerUserId,
        type = type,
        rating = rating,
        comment = comment,
        createdAt = createdAt,
    )
