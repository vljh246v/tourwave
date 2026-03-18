package com.demo.tourwave.adapter.out.persistence.jpa.customer

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(
    name = "favorites",
    indexes = [Index(name = "idx_favorites_user_created", columnList = "user_id,created_at")]
)
data class FavoriteJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @Column(name = "user_id", nullable = false)
    val userId: Long,
    @Column(name = "tour_id", nullable = false)
    val tourId: Long,
    @Column(name = "created_at", nullable = false)
    val createdAt: Instant
)
