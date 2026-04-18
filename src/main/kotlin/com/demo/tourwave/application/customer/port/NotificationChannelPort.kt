package com.demo.tourwave.application.customer.port

import com.demo.tourwave.domain.customer.NotificationChannel

data class SendNotificationMessage(
    val channel: NotificationChannel,
    val recipient: String,
    val subject: String,
    val body: String,
    val templateCode: String,
    val idempotencyKey: String,
)

data class NotificationChannelSendResult(
    val providerMessageId: String?,
)

class NotificationChannelException(
    override val message: String,
    val retryable: Boolean,
) : RuntimeException(message)

interface NotificationChannelPort {
    fun send(message: SendNotificationMessage): NotificationChannelSendResult
}
