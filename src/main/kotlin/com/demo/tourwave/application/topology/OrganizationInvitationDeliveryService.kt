package com.demo.tourwave.application.topology

import com.demo.tourwave.application.auth.UserActionTokenService
import com.demo.tourwave.application.customer.DeliverNotificationCommand
import com.demo.tourwave.application.customer.NotificationDeliveryService
import com.demo.tourwave.application.customer.NotificationTemplateFactory
import com.demo.tourwave.application.topology.port.OrganizationRepository
import com.demo.tourwave.application.user.port.UserRepository
import com.demo.tourwave.domain.auth.UserActionTokenPurpose
import com.demo.tourwave.domain.common.DomainException
import com.demo.tourwave.domain.common.ErrorCode
import com.demo.tourwave.domain.customer.NotificationChannel
import com.demo.tourwave.domain.organization.OrganizationMembership
import java.time.Clock
import java.time.Duration

data class OrganizationInvitationPayload(
    val organizationId: Long,
    val organizationName: String,
    val inviteeUserId: Long,
    val inviteeEmail: String,
    val role: String,
    val acceptUrl: String,
    val expiresAtUtc: java.time.Instant
)

class OrganizationInvitationDeliveryService(
    private val userRepository: UserRepository,
    private val organizationRepository: OrganizationRepository,
    private val userActionTokenService: UserActionTokenService,
    private val notificationDeliveryService: NotificationDeliveryService,
    private val notificationTemplateFactory: NotificationTemplateFactory,
    private val appBaseUrl: String,
    private val invitationTokenTtl: Duration,
    private val clock: Clock
) {
    fun sendInvitation(membership: OrganizationMembership) {
        val invitee = userRepository.findById(membership.userId) ?: throw notFound("user ${membership.userId} not found")
        val organization = organizationRepository.findById(membership.organizationId)
            ?: throw notFound("organization ${membership.organizationId} not found")
        val rawToken = userActionTokenService.issue(
            userId = requireNotNull(invitee.id),
            purpose = UserActionTokenPurpose.ORG_INVITATION,
            ttl = invitationTokenTtl
        )
        val expiresAtUtc = clock.instant().plus(invitationTokenTtl)
        val payload = OrganizationInvitationPayload(
            organizationId = membership.organizationId,
            organizationName = organization.name,
            inviteeUserId = requireNotNull(invitee.id),
            inviteeEmail = invitee.email,
            role = membership.role.name,
            acceptUrl = "${appBaseUrl.trimEnd('/')}/organizations/${membership.organizationId}/memberships/accept?token=$rawToken",
            expiresAtUtc = expiresAtUtc
        )
        val template = notificationTemplateFactory.renderOrganizationInvitation(payload)
        notificationDeliveryService.deliver(
            DeliverNotificationCommand(
                channel = NotificationChannel.EMAIL,
                templateCode = template.templateCode,
                recipient = invitee.email,
                subject = template.subject,
                body = template.body,
                resourceType = "ORGANIZATION_MEMBERSHIP",
                resourceId = membership.organizationId,
                idempotencyKey = "org-invite:${membership.organizationId}:${membership.userId}:${membership.role}:${membership.updatedAt}"
            )
        )
    }

    fun consumeInvitationToken(actorUserId: Long, rawToken: String) {
        val token = userActionTokenService.consume(rawToken, UserActionTokenPurpose.ORG_INVITATION)
        if (token.userId != actorUserId) {
            throw DomainException(
                errorCode = ErrorCode.FORBIDDEN,
                status = 403,
                message = "organization invitation token does not belong to actor"
            )
        }
    }

    private fun notFound(message: String) = DomainException(
        errorCode = ErrorCode.VALIDATION_ERROR,
        status = 404,
        message = message
    )
}
