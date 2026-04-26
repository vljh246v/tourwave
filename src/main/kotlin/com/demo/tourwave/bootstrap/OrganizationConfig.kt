package com.demo.tourwave.bootstrap

import com.demo.tourwave.application.auth.UserActionTokenService
import com.demo.tourwave.application.common.port.AuditEventPort
import com.demo.tourwave.application.customer.NotificationDeliveryService
import com.demo.tourwave.application.customer.NotificationTemplateFactory
import com.demo.tourwave.application.organization.OrganizationAccessGuard
import com.demo.tourwave.application.organization.OrganizationCommandService
import com.demo.tourwave.application.organization.OrganizationInvitationDeliveryService
import com.demo.tourwave.application.organization.OrganizationMembershipService
import com.demo.tourwave.application.organization.OrganizationQueryService
import com.demo.tourwave.application.organization.port.OrganizationMembershipRepository
import com.demo.tourwave.application.organization.port.OrganizationRepository
import com.demo.tourwave.application.user.port.UserRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock
import java.time.Duration

@Configuration
class OrganizationConfig {
    @Bean
    fun organizationAccessGuard(
        organizationRepository: OrganizationRepository,
        membershipRepository: OrganizationMembershipRepository,
    ): OrganizationAccessGuard =
        OrganizationAccessGuard(
            organizationRepository = organizationRepository,
            membershipRepository = membershipRepository,
        )

    @Bean
    fun organizationCommandService(
        organizationRepository: OrganizationRepository,
        membershipRepository: OrganizationMembershipRepository,
        userRepository: UserRepository,
        organizationAccessGuard: OrganizationAccessGuard,
        auditEventPort: AuditEventPort,
        clock: Clock,
    ): OrganizationCommandService =
        OrganizationCommandService(
            organizationRepository = organizationRepository,
            membershipRepository = membershipRepository,
            userRepository = userRepository,
            organizationAccessGuard = organizationAccessGuard,
            auditEventPort = auditEventPort,
            clock = clock,
        )

    @Bean
    fun organizationMembershipService(
        membershipRepository: OrganizationMembershipRepository,
        userRepository: UserRepository,
        organizationAccessGuard: OrganizationAccessGuard,
        organizationInvitationDeliveryService: OrganizationInvitationDeliveryService,
        auditEventPort: AuditEventPort,
        clock: Clock,
    ): OrganizationMembershipService =
        OrganizationMembershipService(
            membershipRepository = membershipRepository,
            userRepository = userRepository,
            organizationAccessGuard = organizationAccessGuard,
            organizationInvitationDeliveryService = organizationInvitationDeliveryService,
            auditEventPort = auditEventPort,
            clock = clock,
        )

    @Bean
    fun organizationInvitationDeliveryService(
        userRepository: UserRepository,
        organizationRepository: OrganizationRepository,
        userActionTokenService: UserActionTokenService,
        notificationDeliveryService: NotificationDeliveryService,
        notificationTemplateFactory: NotificationTemplateFactory,
        @Value("\${tourwave.app.base-url:http://localhost:3000}") appBaseUrl: String,
        @Value("\${tourwave.organization.invitation-token-ttl-seconds:604800}") invitationTokenTtlSeconds: Long,
        clock: Clock,
    ): OrganizationInvitationDeliveryService =
        OrganizationInvitationDeliveryService(
            userRepository = userRepository,
            organizationRepository = organizationRepository,
            userActionTokenService = userActionTokenService,
            notificationDeliveryService = notificationDeliveryService,
            notificationTemplateFactory = notificationTemplateFactory,
            appBaseUrl = appBaseUrl,
            invitationTokenTtl = Duration.ofSeconds(invitationTokenTtlSeconds),
            clock = clock,
        )

    @Bean
    fun organizationQueryService(
        organizationRepository: OrganizationRepository,
        membershipRepository: OrganizationMembershipRepository,
        organizationAccessGuard: OrganizationAccessGuard,
    ): OrganizationQueryService =
        OrganizationQueryService(
            organizationRepository = organizationRepository,
            membershipRepository = membershipRepository,
            organizationAccessGuard = organizationAccessGuard,
        )
}
