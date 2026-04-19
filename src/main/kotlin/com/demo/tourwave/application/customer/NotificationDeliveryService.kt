package com.demo.tourwave.application.customer

import com.demo.tourwave.application.customer.port.NotificationChannelException
import com.demo.tourwave.application.customer.port.NotificationChannelPort
import com.demo.tourwave.application.customer.port.NotificationDeliveryRepository
import com.demo.tourwave.application.customer.port.SendNotificationMessage
import com.demo.tourwave.domain.customer.NotificationChannel
import com.demo.tourwave.domain.customer.NotificationDelivery
import java.time.Clock

data class DeliverNotificationCommand(
    val channel: NotificationChannel,
    val templateCode: String,
    val recipient: String,
    val subject: String,
    val body: String,
    val resourceType: String,
    val resourceId: Long,
    val idempotencyKey: String,
)

class NotificationDeliveryService(
    private val notificationDeliveryRepository: NotificationDeliveryRepository,
    private val notificationChannelPort: NotificationChannelPort,
    private val clock: Clock,
) {
    fun deliver(command: DeliverNotificationCommand): NotificationDelivery {
        val now = clock.instant()
        val pending =
            notificationDeliveryRepository.save(
                NotificationDelivery(
                    channel = command.channel,
                    templateCode = command.templateCode,
                    recipient = command.recipient,
                    subject = command.subject,
                    body = command.body,
                    resourceType = command.resourceType,
                    resourceId = command.resourceId,
                    createdAt = now,
                    updatedAt = now,
                ),
            )
        return try {
            val result =
                notificationChannelPort.send(
                    SendNotificationMessage(
                        channel = command.channel,
                        recipient = command.recipient,
                        subject = command.subject,
                        body = command.body,
                        templateCode = command.templateCode,
                        idempotencyKey = command.idempotencyKey,
                    ),
                )
            notificationDeliveryRepository.save(pending.markSent(result.providerMessageId, clock.instant()))
        } catch (ex: NotificationChannelException) {
            notificationDeliveryRepository.save(
                pending.markFailed(
                    retryable = ex.retryable,
                    message = ex.message,
                    now = clock.instant(),
                ),
            )
        }
    }

    fun redeliver(deliveryId: Long): NotificationDelivery {
        val existing =
            notificationDeliveryRepository.findById(deliveryId)
                ?: throw IllegalArgumentException("Notification delivery not found: $deliveryId")
        return try {
            val result =
                notificationChannelPort.send(
                    SendNotificationMessage(
                        channel = existing.channel,
                        recipient = existing.recipient,
                        subject = existing.subject,
                        body = existing.body,
                        templateCode = existing.templateCode,
                        idempotencyKey = "${existing.resourceType}:${existing.resourceId}:retry:$deliveryId:${existing.attemptCount + 1}",
                    ),
                )
            notificationDeliveryRepository.save(existing.markSent(result.providerMessageId, clock.instant()))
        } catch (ex: NotificationChannelException) {
            notificationDeliveryRepository.save(
                existing.markFailed(
                    retryable = ex.retryable,
                    message = ex.message,
                    now = clock.instant(),
                ),
            )
        }
    }
}
