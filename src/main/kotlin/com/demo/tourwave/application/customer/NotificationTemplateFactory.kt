package com.demo.tourwave.application.customer

import com.demo.tourwave.application.common.port.AuditEventCommand
import com.demo.tourwave.application.topology.OrganizationInvitationPayload

data class RenderedNotificationTemplate(
    val templateCode: String,
    val subject: String,
    val body: String
)

class NotificationTemplateFactory {
    fun renderAuditEvent(event: AuditEventCommand, title: String, body: String): RenderedNotificationTemplate {
        val templateCode = when {
            event.resourceType == "BOOKING" && event.action.contains("REFUND") -> "refund-update"
            event.resourceType == "BOOKING" -> "booking-update"
            event.resourceType == "INQUIRY" || event.resourceType == "INQUIRY_MESSAGE" -> "inquiry-update"
            else -> "generic-update"
        }
        return RenderedNotificationTemplate(
            templateCode = templateCode,
            subject = title,
            body = body
        )
    }

    fun renderOrganizationInvitation(payload: OrganizationInvitationPayload): RenderedNotificationTemplate {
        return RenderedNotificationTemplate(
            templateCode = "organization-invitation",
            subject = "Invitation to join ${payload.organizationName}",
            body = buildString {
                append("You were invited to join ${payload.organizationName} as ${payload.role}. ")
                append("Open ${payload.acceptUrl} before ${payload.expiresAtUtc}.")
            }
        )
    }
}
