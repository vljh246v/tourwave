package com.demo.tourwave.domain.customer

import java.time.Instant

enum class NotificationChannel {
    EMAIL
}

enum class NotificationDeliveryStatus {
    PENDING,
    SENT,
    FAILED_RETRYABLE,
    FAILED_PERMANENT
}

data class NotificationDelivery(
    val id: Long? = null,
    val channel: NotificationChannel,
    val templateCode: String,
    val recipient: String,
    val subject: String,
    val body: String,
    val resourceType: String,
    val resourceId: Long,
    val status: NotificationDeliveryStatus = NotificationDeliveryStatus.PENDING,
    val attemptCount: Int = 0,
    val providerMessageId: String? = null,
    val lastError: String? = null,
    val sentAt: Instant? = null,
    val createdAt: Instant,
    val updatedAt: Instant = createdAt
) {
    fun markSent(providerMessageId: String?, now: Instant): NotificationDelivery {
        return copy(
            status = NotificationDeliveryStatus.SENT,
            attemptCount = attemptCount + 1,
            providerMessageId = providerMessageId,
            lastError = null,
            sentAt = now,
            updatedAt = now
        )
    }

    fun markFailed(retryable: Boolean, message: String, now: Instant): NotificationDelivery {
        return copy(
            status = if (retryable) NotificationDeliveryStatus.FAILED_RETRYABLE else NotificationDeliveryStatus.FAILED_PERMANENT,
            attemptCount = attemptCount + 1,
            lastError = message,
            updatedAt = now
        )
    }
}
