package com.demo.tourwave.adapter.out.persistence.jpa.customer

import com.demo.tourwave.domain.customer.NotificationType
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
    name = "notifications",
    indexes = [
        Index(name = "idx_notifications_user_created", columnList = "user_id,created_at"),
        Index(name = "idx_notifications_user_read", columnList = "user_id,read_at"),
    ],
)
data class NotificationJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @Column(name = "user_id", nullable = false)
    val userId: Long,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val type: NotificationType,
    @Column(nullable = false)
    val title: String,
    @Column(columnDefinition = "TEXT", nullable = false)
    val body: String,
    @Column(name = "resource_type", nullable = false)
    val resourceType: String,
    @Column(name = "resource_id", nullable = false)
    val resourceId: Long,
    @Column(name = "read_at")
    val readAt: Instant? = null,
    @Column(name = "created_at", nullable = false)
    val createdAt: Instant,
)
