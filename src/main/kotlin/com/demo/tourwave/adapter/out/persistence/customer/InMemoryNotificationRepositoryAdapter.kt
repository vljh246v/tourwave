package com.demo.tourwave.adapter.out.persistence.customer

import com.demo.tourwave.application.customer.port.NotificationRepository
import com.demo.tourwave.domain.customer.Notification
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

@Repository
@Profile("!mysql & !mysql-test")
class InMemoryNotificationRepositoryAdapter : NotificationRepository {
    private val sequence = AtomicLong(0)
    private val notifications = ConcurrentHashMap<Long, Notification>()

    override fun save(notification: Notification): Notification {
        val id = notification.id ?: sequence.incrementAndGet()
        val saved = notification.copy(id = id)
        notifications[id] = saved
        return saved
    }

    override fun findById(notificationId: Long): Notification? = notifications[notificationId]

    override fun findByUserId(userId: Long): List<Notification> =
        notifications.values.filter { it.userId == userId }.sortedByDescending { it.createdAt }

    override fun markAllRead(
        userId: Long,
        notificationIds: List<Long>,
    ) {
        val now = Instant.now()
        notificationIds.forEach { id ->
            val notification = notifications[id] ?: return@forEach
            if (notification.userId == userId) {
                notifications[id] = notification.markRead(now)
            }
        }
    }

    override fun clear() {
        notifications.clear()
        sequence.set(0)
    }
}
