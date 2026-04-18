package com.demo.tourwave.adapter.out.persistence.jpa.customer

import org.springframework.data.jpa.repository.JpaRepository

interface FavoriteJpaRepository : JpaRepository<FavoriteJpaEntity, Long> {
    fun findByUserIdOrderByCreatedAtDesc(userId: Long): List<FavoriteJpaEntity>

    fun findByUserIdAndTourId(
        userId: Long,
        tourId: Long,
    ): FavoriteJpaEntity?
}
