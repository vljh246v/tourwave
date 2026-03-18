package com.demo.tourwave.adapter.out.persistence.jpa.customer

import com.demo.tourwave.application.customer.port.NotificationRepository
import com.demo.tourwave.domain.customer.Notification
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
@Profile("mysql", "mysql-test")
class JpaNotificationRepositoryAdapter(
    private val notificationJpaRepository: NotificationJpaRepository
) : NotificationRepository {
    override fun save(notification: Notification): Notification =
        notificationJpaRepository.save(notification.toEntity()).toDomain()

    override fun findById(notificationId: Long): Notification? =
        notificationJpaRepository.findById(notificationId).orElse(null)?.toDomain()

    override fun findByUserId(userId: Long): List<Notification> =
        notificationJpaRepository.findByUserIdOrderByCreatedAtDesc(userId).map { it.toDomain() }

    override fun markAllRead(userId: Long, notificationIds: List<Long>) {
        if (notificationIds.isEmpty()) return
        val now = Instant.now()
        val entities = notificationJpaRepository.findAllById(notificationIds).filter { it.userId == userId }
        notificationJpaRepository.saveAll(entities.map { it.copy(readAt = it.readAt ?: now) })
    }

    override fun clear() {
        notificationJpaRepository.deleteAllInBatch()
    }
}

private fun Notification.toEntity(): NotificationJpaEntity =
    NotificationJpaEntity(
        id = id,
        userId = userId,
        type = type,
        title = title,
        body = body,
        resourceType = resourceType,
        resourceId = resourceId,
        readAt = readAt,
        createdAt = createdAt
    )

private fun NotificationJpaEntity.toDomain(): Notification =
    Notification(
        id = id,
        userId = userId,
        type = type,
        title = title,
        body = body,
        resourceType = resourceType,
        resourceId = resourceId,
        readAt = readAt,
        createdAt = createdAt
    )
