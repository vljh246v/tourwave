package com.demo.tourwave.bootstrap

import com.demo.tourwave.application.asset.AssetCommandService
import com.demo.tourwave.application.asset.port.AssetRepository
import com.demo.tourwave.application.asset.port.AssetStoragePort
import com.demo.tourwave.application.booking.port.BookingRepository
import com.demo.tourwave.application.booking.port.OccurrenceRepository
import com.demo.tourwave.application.booking.port.PaymentRecordRepository
import com.demo.tourwave.application.customer.CustomerBookingQueryService
import com.demo.tourwave.application.customer.FavoriteService
import com.demo.tourwave.application.customer.NotificationDeliveryService
import com.demo.tourwave.application.customer.NotificationService
import com.demo.tourwave.application.customer.NotificationTemplateFactory
import com.demo.tourwave.application.customer.port.NotificationChannelPort
import com.demo.tourwave.application.customer.port.NotificationDeliveryRepository
import com.demo.tourwave.application.customer.port.FavoriteRepository
import com.demo.tourwave.application.customer.port.NotificationRepository
import com.demo.tourwave.application.inquiry.port.InquiryRepository
import com.demo.tourwave.application.participant.ParticipantAccessPolicy
import com.demo.tourwave.application.participant.port.BookingParticipantRepository
import com.demo.tourwave.application.organization.OrganizationAccessGuard
import com.demo.tourwave.application.organization.port.OrganizationRepository
import com.demo.tourwave.application.topology.port.TourRepository
import com.demo.tourwave.application.user.port.UserRepository
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock

@Configuration
class CustomerConfig {
    @Bean
    fun assetCommandService(
        assetRepository: AssetRepository,
        assetStoragePort: AssetStoragePort,
        organizationRepository: OrganizationRepository,
        tourRepository: TourRepository,
        organizationAccessGuard: OrganizationAccessGuard,
        userRepository: UserRepository,
        clock: Clock
    ): AssetCommandService {
        return AssetCommandService(
            assetRepository = assetRepository,
            assetStoragePort = assetStoragePort,
            organizationRepository = organizationRepository,
            tourRepository = tourRepository,
            organizationAccessGuard = organizationAccessGuard,
            userRepository = userRepository,
            clock = clock
        )
    }

    @Bean
    fun customerBookingQueryService(
        bookingRepository: BookingRepository,
        occurrenceRepository: OccurrenceRepository,
        bookingParticipantRepository: BookingParticipantRepository,
        participantAccessPolicy: ParticipantAccessPolicy,
        tourRepository: TourRepository,
        organizationRepository: OrganizationRepository
    ): CustomerBookingQueryService {
        return CustomerBookingQueryService(
            bookingRepository = bookingRepository,
            occurrenceRepository = occurrenceRepository,
            bookingParticipantRepository = bookingParticipantRepository,
            participantAccessPolicy = participantAccessPolicy,
            tourRepository = tourRepository,
            organizationRepository = organizationRepository
        )
    }

    @Bean
    fun favoriteService(
        favoriteRepository: FavoriteRepository,
        tourRepository: TourRepository,
        userRepository: UserRepository,
        clock: Clock
    ): FavoriteService {
        return FavoriteService(
            favoriteRepository = favoriteRepository,
            tourRepository = tourRepository,
            userRepository = userRepository,
            clock = clock
        )
    }

    @Bean
    fun notificationService(
        notificationRepository: NotificationRepository,
        notificationDeliveryService: NotificationDeliveryService,
        bookingRepository: BookingRepository,
        inquiryRepository: InquiryRepository,
        paymentRecordRepository: PaymentRecordRepository,
        userRepository: UserRepository,
        notificationTemplateFactory: NotificationTemplateFactory,
        clock: Clock
    ): NotificationService {
        return NotificationService(
            notificationRepository = notificationRepository,
            notificationDeliveryService = notificationDeliveryService,
            bookingRepository = bookingRepository,
            inquiryRepository = inquiryRepository,
            paymentRecordRepository = paymentRecordRepository,
            userRepository = userRepository,
            notificationTemplateFactory = notificationTemplateFactory,
            clock = clock
        )
    }

    @Bean
    fun notificationTemplateFactory(): NotificationTemplateFactory {
        return NotificationTemplateFactory()
    }

    @Bean
    fun notificationDeliveryService(
        notificationDeliveryRepository: NotificationDeliveryRepository,
        notificationChannelPort: NotificationChannelPort,
        clock: Clock
    ): NotificationDeliveryService {
        return NotificationDeliveryService(
            notificationDeliveryRepository = notificationDeliveryRepository,
            notificationChannelPort = notificationChannelPort,
            clock = clock
        )
    }
}
