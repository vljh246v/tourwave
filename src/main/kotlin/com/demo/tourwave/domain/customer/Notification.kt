package com.demo.tourwave.domain.customer

import java.time.Instant

enum class NotificationType {
    BOOKING,
    INQUIRY,
    REFUND,
}

data class Notification(
    val id: Long? = null,
    val userId: Long,
    val type: NotificationType,
    val title: String,
    val body: String,
    val resourceType: String,
    val resourceId: Long,
    val readAt: Instant? = null,
    val createdAt: Instant,
) {
    fun markRead(now: Instant): Notification = copy(readAt = readAt ?: now)
}
