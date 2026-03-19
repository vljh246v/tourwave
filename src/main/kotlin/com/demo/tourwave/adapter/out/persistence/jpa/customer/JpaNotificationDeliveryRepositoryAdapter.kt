package com.demo.tourwave.adapter.out.persistence.jpa.customer

import com.demo.tourwave.application.customer.port.NotificationDeliveryRepository
import com.demo.tourwave.domain.customer.NotificationDelivery
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component
@Profile("mysql", "mysql-test")
class JpaNotificationDeliveryRepositoryAdapter(
    private val notificationDeliveryJpaRepository: NotificationDeliveryJpaRepository
) : NotificationDeliveryRepository {
    override fun save(delivery: NotificationDelivery): NotificationDelivery =
        notificationDeliveryJpaRepository.save(delivery.toEntity()).toDomain()

    override fun findById(id: Long): NotificationDelivery? =
        notificationDeliveryJpaRepository.findById(id).orElse(null)?.toDomain()

    override fun findAll(): List<NotificationDelivery> =
        notificationDeliveryJpaRepository.findAll().map { it.toDomain() }

    override fun clear() {
        notificationDeliveryJpaRepository.deleteAllInBatch()
    }
}

private fun NotificationDelivery.toEntity(): NotificationDeliveryJpaEntity =
    NotificationDeliveryJpaEntity(
        id = id,
        channel = channel,
        templateCode = templateCode,
        recipient = recipient,
        subject = subject,
        body = body,
        resourceType = resourceType,
        resourceId = resourceId,
        status = status,
        attemptCount = attemptCount,
        providerMessageId = providerMessageId,
        lastError = lastError,
        sentAt = sentAt,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

private fun NotificationDeliveryJpaEntity.toDomain(): NotificationDelivery =
    NotificationDelivery(
        id = id,
        channel = channel,
        templateCode = templateCode,
        recipient = recipient,
        subject = subject,
        body = body,
        resourceType = resourceType,
        resourceId = resourceId,
        status = status,
        attemptCount = attemptCount,
        providerMessageId = providerMessageId,
        lastError = lastError,
        sentAt = sentAt,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
