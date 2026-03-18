package com.demo.tourwave.application.customer

import com.demo.tourwave.application.booking.port.BookingRepository
import com.demo.tourwave.application.booking.port.PaymentRecordRepository
import com.demo.tourwave.application.common.port.AuditEventCommand
import com.demo.tourwave.application.common.port.AuditEventSubscriber
import com.demo.tourwave.application.customer.port.NotificationRepository
import com.demo.tourwave.application.inquiry.port.InquiryRepository
import com.demo.tourwave.application.user.port.UserRepository
import com.demo.tourwave.domain.common.DomainException
import com.demo.tourwave.domain.common.ErrorCode
import com.demo.tourwave.domain.customer.Notification
import com.demo.tourwave.domain.customer.NotificationChannel
import com.demo.tourwave.domain.customer.NotificationType
import java.time.Clock

class NotificationService(
    private val notificationRepository: NotificationRepository,
    private val notificationDeliveryService: NotificationDeliveryService,
    private val bookingRepository: BookingRepository,
    private val inquiryRepository: InquiryRepository,
    private val paymentRecordRepository: PaymentRecordRepository,
    private val userRepository: UserRepository,
    private val notificationTemplateFactory: NotificationTemplateFactory,
    private val clock: Clock,
) : AuditEventSubscriber {
    fun list(userId: Long): List<Notification> = notificationRepository.findByUserId(userId)

    fun markRead(
        userId: Long,
        notificationId: Long,
    ): Notification {
        val notification = notificationRepository.findById(notificationId) ?: throw notFound("notification $notificationId not found")
        if (notification.userId != userId) {
            throw DomainException(
                errorCode = ErrorCode.FORBIDDEN,
                status = 403,
                message = "notification does not belong to actor",
            )
        }
        val updated = notification.markRead(clock.instant())
        return notificationRepository.save(updated)
    }

    fun markAllRead(userId: Long): List<Notification> {
        val notifications = notificationRepository.findByUserId(userId)
        val unreadIds = notifications.filter { it.readAt == null }.map { requireNotNull(it.id) }
        notificationRepository.markAllRead(userId, unreadIds)
        return notificationRepository.findByUserId(userId)
    }

    override fun handle(event: AuditEventCommand) {
        val projection = project(event) ?: return
        val notification =
            notificationRepository.save(
                Notification(
                    userId = projection.userId,
                    type = projection.type,
                    title = projection.title,
                    body = projection.body,
                    resourceType = event.resourceType,
                    resourceId = event.resourceId,
                    createdAt = event.occurredAtUtc,
                ),
            )
        val user = userRepository.findById(projection.userId) ?: return
        val template = notificationTemplateFactory.renderAuditEvent(event, projection.title, projection.body)
        notificationDeliveryService.deliver(
            DeliverNotificationCommand(
                channel = NotificationChannel.EMAIL,
                templateCode = template.templateCode,
                recipient = user.email,
                subject = template.subject,
                body = template.body,
                resourceType = notification.resourceType,
                resourceId = notification.resourceId,
                idempotencyKey = "notification:${event.resourceType}:${event.resourceId}:${event.action}:${projection.userId}",
            ),
        )
    }

    private fun project(event: AuditEventCommand): NotificationProjection? {
        return when (event.resourceType) {
            "BOOKING" -> {
                val booking = bookingRepository.findById(event.resourceId) ?: return null
                val type = if (event.action.contains("REFUND")) NotificationType.REFUND else NotificationType.BOOKING
                NotificationProjection(
                    userId = booking.leaderUserId,
                    type = type,
                    title =
                        when (type) {
                            NotificationType.REFUND -> "Refund update"
                            else -> "Booking update"
                        },
                    body = "${event.action.replace('_', ' ')} for booking ${booking.id}",
                )
            }

            "INQUIRY" -> {
                val inquiry = inquiryRepository.findById(event.resourceId) ?: return null
                NotificationProjection(
                    userId = inquiry.createdByUserId,
                    type = NotificationType.INQUIRY,
                    title = "Inquiry update",
                    body = "${event.action.replace('_', ' ')} on inquiry ${inquiry.id}",
                )
            }

            "INQUIRY_MESSAGE" -> {
                val message = inquiryRepository.findMessageById(event.resourceId) ?: return null
                val inquiry = inquiryRepository.findById(message.inquiryId) ?: return null
                if (inquiry.createdByUserId == message.senderUserId) return null
                NotificationProjection(
                    userId = inquiry.createdByUserId,
                    type = NotificationType.INQUIRY,
                    title = "New inquiry message",
                    body = "A new inquiry message was posted on inquiry ${inquiry.id}",
                )
            }

            "PAYMENT_RECORD" -> {
                val bookingId = (event.afterJson?.get("bookingId") as? Number)?.toLong()
                val booking = bookingId?.let(bookingRepository::findById) ?: return null
                NotificationProjection(
                    userId = booking.leaderUserId,
                    type = NotificationType.REFUND,
                    title = "Refund processing update",
                    body = "${event.action.replace('_', ' ')} for booking ${booking.id}",
                )
            }

            else -> {
                null
            }
        }
    }

    private fun notFound(message: String) =
        DomainException(
            errorCode = ErrorCode.VALIDATION_ERROR,
            status = 404,
            message = message,
        )
}

private data class NotificationProjection(
    val userId: Long,
    val type: NotificationType,
    val title: String,
    val body: String,
)
