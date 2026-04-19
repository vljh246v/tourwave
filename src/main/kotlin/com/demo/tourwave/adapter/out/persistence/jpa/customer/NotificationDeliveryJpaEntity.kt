package com.demo.tourwave.adapter.out.persistence.jpa.customer

import com.demo.tourwave.domain.customer.NotificationChannel
import com.demo.tourwave.domain.customer.NotificationDeliveryStatus
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
    name = "notification_deliveries",
    indexes = [
        Index(name = "idx_notification_deliveries_resource", columnList = "resource_type,resource_id"),
        Index(name = "idx_notification_deliveries_status_created", columnList = "status,created_at"),
    ],
)
data class NotificationDeliveryJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val channel: NotificationChannel,
    @Column(name = "template_code", nullable = false)
    val templateCode: String,
    @Column(nullable = false)
    val recipient: String,
    @Column(nullable = false)
    val subject: String,
    @Column(nullable = false, columnDefinition = "TEXT")
    val body: String,
    @Column(name = "resource_type", nullable = false)
    val resourceType: String,
    @Column(name = "resource_id", nullable = false)
    val resourceId: Long,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val status: NotificationDeliveryStatus,
    @Column(name = "attempt_count", nullable = false)
    val attemptCount: Int,
    @Column(name = "provider_message_id")
    val providerMessageId: String? = null,
    @Column(name = "last_error")
    val lastError: String? = null,
    @Column(name = "sent_at")
    val sentAt: Instant? = null,
    @Column(name = "created_at", nullable = false)
    val createdAt: Instant,
    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant,
)
