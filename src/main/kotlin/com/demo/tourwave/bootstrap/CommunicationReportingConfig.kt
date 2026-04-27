package com.demo.tourwave.bootstrap

import com.demo.tourwave.application.announcement.AnnouncementService
import com.demo.tourwave.application.announcement.port.AnnouncementRepository
import com.demo.tourwave.application.booking.port.BookingRepository
import com.demo.tourwave.application.booking.port.OccurrenceRepository
import com.demo.tourwave.application.booking.port.PaymentRecordRepository
import com.demo.tourwave.application.common.port.AuditEventPort
import com.demo.tourwave.application.common.port.IdempotencyStore
import com.demo.tourwave.application.organization.OrganizationAccessGuard
import com.demo.tourwave.application.participant.port.BookingParticipantRepository
import com.demo.tourwave.application.reporting.OrganizationReportService
import com.demo.tourwave.application.tour.port.TourRepository
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock

@Configuration
class CommunicationReportingConfig {
    @Bean
    fun announcementService(
        announcementRepository: AnnouncementRepository,
        organizationAccessGuard: OrganizationAccessGuard,
        auditEventPort: AuditEventPort,
        idempotencyStore: IdempotencyStore,
        clock: Clock,
    ): AnnouncementService {
        return AnnouncementService(
            announcementRepository = announcementRepository,
            organizationAccessGuard = organizationAccessGuard,
            auditEventPort = auditEventPort,
            idempotencyStore = idempotencyStore,
            clock = clock,
        )
    }

    @Bean
    fun organizationReportService(
        bookingRepository: BookingRepository,
        occurrenceRepository: OccurrenceRepository,
        bookingParticipantRepository: BookingParticipantRepository,
        paymentRecordRepository: PaymentRecordRepository,
        tourRepository: TourRepository,
        organizationAccessGuard: OrganizationAccessGuard,
    ): OrganizationReportService {
        return OrganizationReportService(
            bookingRepository = bookingRepository,
            occurrenceRepository = occurrenceRepository,
            bookingParticipantRepository = bookingParticipantRepository,
            paymentRecordRepository = paymentRecordRepository,
            tourRepository = tourRepository,
            organizationAccessGuard = organizationAccessGuard,
        )
    }
}
