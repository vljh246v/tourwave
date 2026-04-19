package com.demo.tourwave.adapter.out.persistence.review

import com.demo.tourwave.application.review.port.ReviewRepository
import com.demo.tourwave.domain.review.Review
import com.demo.tourwave.domain.review.ReviewType
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Repository
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

@Repository
@Profile("!mysql & !mysql-test")
class InMemoryReviewRepositoryAdapter : ReviewRepository {
    private val sequence = AtomicLong(0)
    private val reviews = ConcurrentHashMap<Long, Review>()

    override fun save(review: Review): Review {
        val reviewId = review.id ?: sequence.incrementAndGet()
        val saved = review.copy(id = reviewId)
        reviews[reviewId] = saved
        return saved
    }

    override fun findByOccurrenceAndReviewerAndType(
        occurrenceId: Long,
        reviewerUserId: Long,
        type: ReviewType,
    ): Review? {
        return reviews.values.firstOrNull {
            it.occurrenceId == occurrenceId &&
                it.reviewerUserId == reviewerUserId &&
                it.type == type
        }
    }

    override fun findByOccurrenceAndType(
        occurrenceId: Long,
        type: ReviewType,
    ): List<Review> {
        return reviews.values
            .filter { it.occurrenceId == occurrenceId && it.type == type }
            .sortedBy { it.createdAt }
    }

    override fun findAll(): List<Review> = reviews.values.sortedBy { it.id }

    override fun clear() {
        reviews.clear()
        sequence.set(0)
    }
}
