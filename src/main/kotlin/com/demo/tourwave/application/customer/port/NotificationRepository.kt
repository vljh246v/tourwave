package com.demo.tourwave.application.customer.port

import com.demo.tourwave.domain.customer.Notification

interface NotificationRepository {
    fun save(notification: Notification): Notification
    fun findById(notificationId: Long): Notification?
    fun findByUserId(userId: Long): List<Notification>
    fun markAllRead(userId: Long, notificationIds: List<Long>)
    fun clear()
}
