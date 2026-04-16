package com.demo.tourwave.bootstrap

import com.demo.tourwave.application.organization.OrganizationAccessGuard
import com.demo.tourwave.application.organization.OrganizationCommandService
import com.demo.tourwave.application.organization.OrganizationInvitationDeliveryService
import com.demo.tourwave.application.organization.OrganizationMembershipService
import com.demo.tourwave.application.organization.OrganizationQueryService
import com.demo.tourwave.application.topology.InstructorProfileService
import com.demo.tourwave.application.topology.InstructorRegistrationService
import com.demo.tourwave.application.topology.CatalogQueryService
import com.demo.tourwave.application.topology.OccurrenceCommandService
import com.demo.tourwave.application.topology.TourCommandService
import com.demo.tourwave.application.topology.TourQueryService
import com.demo.tourwave.application.booking.port.BookingRepository
import com.demo.tourwave.application.booking.port.OccurrenceRepository
import com.demo.tourwave.application.common.TimeWindowPolicyService
import com.demo.tourwave.application.review.port.ReviewRepository
import com.demo.tourwave.application.topology.port.InstructorProfileRepository
import com.demo.tourwave.application.topology.port.InstructorRegistrationRepository
import com.demo.tourwave.application.organization.port.OrganizationMembershipRepository
import com.demo.tourwave.application.organization.port.OrganizationRepository
import com.demo.tourwave.application.topology.port.TourRepository
import com.demo.tourwave.application.auth.UserActionTokenService
import com.demo.tourwave.application.customer.NotificationDeliveryService
import com.demo.tourwave.application.customer.NotificationTemplateFactory
import com.demo.tourwave.application.user.port.UserRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock
import java.time.Duration

@Configuration
class TopologyConfig {
    @Bean
    fun organizationAccessGuard(
        organizationRepository: OrganizationRepository,
        membershipRepository: OrganizationMembershipRepository
    ): OrganizationAccessGuard {
        return OrganizationAccessGuard(
            organizationRepository = organizationRepository,
            membershipRepository = membershipRepository
        )
    }

    @Bean
    fun organizationCommandService(
        organizationRepository: OrganizationRepository,
        membershipRepository: OrganizationMembershipRepository,
        userRepository: UserRepository,
        organizationAccessGuard: OrganizationAccessGuard,
        clock: Clock
    ): OrganizationCommandService {
        return OrganizationCommandService(
            organizationRepository = organizationRepository,
            membershipRepository = membershipRepository,
            userRepository = userRepository,
            organizationAccessGuard = organizationAccessGuard,
            clock = clock
        )
    }

    @Bean
    fun organizationMembershipService(
        membershipRepository: OrganizationMembershipRepository,
        userRepository: UserRepository,
        organizationAccessGuard: OrganizationAccessGuard,
        organizationInvitationDeliveryService: OrganizationInvitationDeliveryService,
        clock: Clock
    ): OrganizationMembershipService {
        return OrganizationMembershipService(
            membershipRepository = membershipRepository,
            userRepository = userRepository,
            organizationAccessGuard = organizationAccessGuard,
            organizationInvitationDeliveryService = organizationInvitationDeliveryService,
            clock = clock
        )
    }

    @Bean
    fun organizationInvitationDeliveryService(
        userRepository: UserRepository,
        organizationRepository: OrganizationRepository,
        userActionTokenService: UserActionTokenService,
        notificationDeliveryService: NotificationDeliveryService,
        notificationTemplateFactory: NotificationTemplateFactory,
        @Value("\${tourwave.app.base-url:http://localhost:3000}") appBaseUrl: String,
        @Value("\${tourwave.organization.invitation-token-ttl-seconds:604800}") invitationTokenTtlSeconds: Long,
        clock: Clock
    ): OrganizationInvitationDeliveryService {
        return OrganizationInvitationDeliveryService(
            userRepository = userRepository,
            organizationRepository = organizationRepository,
            userActionTokenService = userActionTokenService,
            notificationDeliveryService = notificationDeliveryService,
            notificationTemplateFactory = notificationTemplateFactory,
            appBaseUrl = appBaseUrl,
            invitationTokenTtl = Duration.ofSeconds(invitationTokenTtlSeconds),
            clock = clock
        )
    }

    @Bean
    fun organizationQueryService(
        organizationRepository: OrganizationRepository,
        membershipRepository: OrganizationMembershipRepository,
        organizationAccessGuard: OrganizationAccessGuard
    ): OrganizationQueryService {
        return OrganizationQueryService(
            organizationRepository = organizationRepository,
            membershipRepository = membershipRepository,
            organizationAccessGuard = organizationAccessGuard
        )
    }

    @Bean
    fun instructorRegistrationService(
        registrationRepository: InstructorRegistrationRepository,
        instructorProfileRepository: InstructorProfileRepository,
        organizationRepository: OrganizationRepository,
        organizationAccessGuard: OrganizationAccessGuard,
        userRepository: UserRepository,
        clock: Clock
    ): InstructorRegistrationService {
        return InstructorRegistrationService(
            registrationRepository = registrationRepository,
            instructorProfileRepository = instructorProfileRepository,
            organizationRepository = organizationRepository,
            organizationAccessGuard = organizationAccessGuard,
            userRepository = userRepository,
            clock = clock
        )
    }

    @Bean
    fun instructorProfileService(
        instructorProfileRepository: InstructorProfileRepository,
        instructorRegistrationRepository: InstructorRegistrationRepository,
        userRepository: UserRepository,
        clock: Clock
    ): InstructorProfileService {
        return InstructorProfileService(
            instructorProfileRepository = instructorProfileRepository,
            instructorRegistrationRepository = instructorRegistrationRepository,
            userRepository = userRepository,
            clock = clock
        )
    }

    @Bean
    fun tourCommandService(
        tourRepository: TourRepository,
        organizationRepository: OrganizationRepository,
        organizationAccessGuard: OrganizationAccessGuard,
        clock: Clock
    ): TourCommandService {
        return TourCommandService(
            tourRepository = tourRepository,
            organizationRepository = organizationRepository,
            organizationAccessGuard = organizationAccessGuard,
            clock = clock
        )
    }

    @Bean
    fun tourQueryService(
        tourRepository: TourRepository,
        organizationAccessGuard: OrganizationAccessGuard
    ): TourQueryService {
        return TourQueryService(
            tourRepository = tourRepository,
            organizationAccessGuard = organizationAccessGuard
        )
    }

    @Bean
    fun occurrenceCommandService(
        occurrenceRepository: OccurrenceRepository,
        bookingRepository: BookingRepository,
        tourRepository: TourRepository,
        instructorProfileRepository: InstructorProfileRepository,
        organizationAccessGuard: OrganizationAccessGuard,
        clock: Clock
    ): OccurrenceCommandService {
        return OccurrenceCommandService(
            occurrenceRepository = occurrenceRepository,
            bookingRepository = bookingRepository,
            tourRepository = tourRepository,
            instructorProfileRepository = instructorProfileRepository,
            organizationAccessGuard = organizationAccessGuard,
            clock = clock
        )
    }

    @Bean
    fun catalogQueryService(
        tourRepository: TourRepository,
        occurrenceRepository: OccurrenceRepository,
        bookingRepository: BookingRepository,
        reviewRepository: ReviewRepository,
        timeWindowPolicyService: TimeWindowPolicyService
    ): CatalogQueryService {
        return CatalogQueryService(
            tourRepository = tourRepository,
            occurrenceRepository = occurrenceRepository,
            bookingRepository = bookingRepository,
            reviewRepository = reviewRepository,
            timeWindowPolicyService = timeWindowPolicyService
        )
    }
}
