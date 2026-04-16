package com.demo.tourwave.domain.customer

import java.time.Instant

data class Favorite(
    val id: Long? = null,
    val userId: Long,
    val tourId: Long,
    val createdAt: Instant,
)
