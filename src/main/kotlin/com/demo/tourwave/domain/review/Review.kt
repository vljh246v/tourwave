package com.demo.tourwave.domain.review

import java.time.Instant

data class Review(
    val id: Long? = null,
    val occurrenceId: Long,
    val reviewerUserId: Long,
    val type: ReviewType,
    val rating: Int,
    val comment: String? = null,
    val createdAt: Instant = Instant.now(),
) {
    init {
        require(rating in 1..5) { "rating must be between 1 and 5" }
    }
}
