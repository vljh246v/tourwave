package com.demo.tourwave.bootstrap

import com.demo.tourwave.application.booking.BookingCommandService
import com.demo.tourwave.application.booking.WaitlistOperatorService
import com.demo.tourwave.application.booking.BookingQueryService
import com.demo.tourwave.application.booking.port.BookingRepository
import com.demo.tourwave.application.booking.port.OccurrenceRepository
import com.demo.tourwave.application.common.port.AuditEventPort
import com.demo.tourwave.application.common.port.IdempotencyStore
import com.demo.tourwave.application.inquiry.InquiryAccessPolicy
import com.demo.tourwave.application.inquiry.InquiryCommandService
import com.demo.tourwave.application.inquiry.InquiryQueryService
import com.demo.tourwave.application.participant.ParticipantAccessPolicy
import com.demo.tourwave.application.inquiry.port.InquiryRepository
import com.demo.tourwave.application.participant.ParticipantCommandService
import com.demo.tourwave.application.participant.ParticipantInvitationLifecycleService
import com.demo.tourwave.application.participant.ParticipantQueryService
import com.demo.tourwave.application.participant.ParticipantRosterQueryService
import com.demo.tourwave.application.participant.port.BookingParticipantRepository
import com.demo.tourwave.application.review.ReviewCommandService
import com.demo.tourwave.application.review.ReviewQueryService
import com.demo.tourwave.application.review.port.ReviewRepository
import com.demo.tourwave.application.user.UserCommandService
import com.demo.tourwave.application.user.port.UserQueryPort
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock

@Configuration
class UseCaseConfig {
    @Bean
    fun bookingCommandService(
        bookingRepository: BookingRepository,
        occurrenceRepository: OccurrenceRepository,
        bookingParticipantRepository: BookingParticipantRepository,
        idempotencyStore: IdempotencyStore,
        auditEventPort: AuditEventPort,
        clock: Clock
    ): BookingCommandService {
        return BookingCommandService(
            bookingRepository = bookingRepository,
            occurrenceRepository = occurrenceRepository,
            bookingParticipantRepository = bookingParticipantRepository,
            idempotencyStore = idempotencyStore,
            auditEventPort = auditEventPort,
            clock = clock
        )
    }

    @Bean
    fun bookingQueryService(
        bookingRepository: BookingRepository,
        occurrenceRepository: OccurrenceRepository,
        participantAccessPolicy: ParticipantAccessPolicy,
        participantInvitationLifecycleService: ParticipantInvitationLifecycleService
    ): BookingQueryService {
        return BookingQueryService(
            bookingRepository = bookingRepository,
            occurrenceRepository = occurrenceRepository,
            participantAccessPolicy = participantAccessPolicy,
            participantInvitationLifecycleService = participantInvitationLifecycleService
        )
    }

    @Bean
    fun waitlistOperatorService(
        bookingRepository: BookingRepository,
        occurrenceRepository: OccurrenceRepository,
        auditEventPort: AuditEventPort,
        clock: Clock
    ): WaitlistOperatorService {
        return WaitlistOperatorService(
            bookingRepository = bookingRepository,
            occurrenceRepository = occurrenceRepository,
            auditEventPort = auditEventPort,
            clock = clock
        )
    }

    @Bean
    fun inquiryCommandService(
        bookingRepository: BookingRepository,
        inquiryRepository: InquiryRepository,
        inquiryAccessPolicy: InquiryAccessPolicy,
        idempotencyStore: IdempotencyStore,
        auditEventPort: AuditEventPort,
        clock: Clock
    ): InquiryCommandService {
        return InquiryCommandService(
            bookingRepository = bookingRepository,
            inquiryRepository = inquiryRepository,
            inquiryAccessPolicy = inquiryAccessPolicy,
            idempotencyStore = idempotencyStore,
            auditEventPort = auditEventPort,
            clock = clock
        )
    }

    @Bean
    fun inquiryAccessPolicy(
        bookingRepository: BookingRepository
    ): InquiryAccessPolicy {
        return InquiryAccessPolicy(bookingRepository)
    }

    @Bean
    fun participantCommandService(
        bookingRepository: BookingRepository,
        occurrenceRepository: OccurrenceRepository,
        bookingParticipantRepository: BookingParticipantRepository,
        participantInvitationLifecycleService: ParticipantInvitationLifecycleService,
        idempotencyStore: IdempotencyStore,
        auditEventPort: AuditEventPort,
        clock: Clock
    ): ParticipantCommandService {
        return ParticipantCommandService(
            bookingRepository = bookingRepository,
            bookingParticipantRepository = bookingParticipantRepository,
            participantInvitationLifecycleService = participantInvitationLifecycleService,
            idempotencyStore = idempotencyStore,
            auditEventPort = auditEventPort,
            clock = clock
        )
    }

    @Bean
    fun participantInvitationLifecycleService(
        bookingRepository: BookingRepository,
        occurrenceRepository: OccurrenceRepository,
        bookingParticipantRepository: BookingParticipantRepository,
        clock: Clock
    ): ParticipantInvitationLifecycleService {
        return ParticipantInvitationLifecycleService(
            bookingRepository = bookingRepository,
            occurrenceRepository = occurrenceRepository,
            bookingParticipantRepository = bookingParticipantRepository,
            clock = clock
        )
    }

    @Bean
    fun participantAccessPolicy(
        bookingRepository: BookingRepository,
        bookingParticipantRepository: BookingParticipantRepository
    ): ParticipantAccessPolicy {
        return ParticipantAccessPolicy(
            bookingRepository = bookingRepository,
            bookingParticipantRepository = bookingParticipantRepository
        )
    }

    @Bean
    fun participantQueryService(
        participantAccessPolicy: ParticipantAccessPolicy,
        participantInvitationLifecycleService: ParticipantInvitationLifecycleService
    ): ParticipantQueryService {
        return ParticipantQueryService(
            participantAccessPolicy = participantAccessPolicy,
            participantInvitationLifecycleService = participantInvitationLifecycleService
        )
    }

    @Bean
    fun participantRosterQueryService(
        bookingRepository: BookingRepository,
        occurrenceRepository: OccurrenceRepository,
        bookingParticipantRepository: BookingParticipantRepository
    ): ParticipantRosterQueryService {
        return ParticipantRosterQueryService(
            bookingRepository = bookingRepository,
            occurrenceRepository = occurrenceRepository,
            bookingParticipantRepository = bookingParticipantRepository
        )
    }

    @Bean
    fun inquiryQueryService(
        inquiryRepository: InquiryRepository,
        inquiryAccessPolicy: InquiryAccessPolicy
    ): InquiryQueryService {
        return InquiryQueryService(
            inquiryRepository = inquiryRepository,
            inquiryAccessPolicy = inquiryAccessPolicy
        )
    }

    @Bean
    fun userCommandService(
        userQueryPort: UserQueryPort
    ): UserCommandService {
        return UserCommandService(userQueryPort)
    }

    @Bean
    fun reviewCommandService(
        bookingRepository: BookingRepository,
        bookingParticipantRepository: BookingParticipantRepository,
        reviewRepository: ReviewRepository,
        idempotencyStore: IdempotencyStore,
        auditEventPort: AuditEventPort,
        clock: Clock
    ): ReviewCommandService {
        return ReviewCommandService(
            bookingRepository = bookingRepository,
            bookingParticipantRepository = bookingParticipantRepository,
            reviewRepository = reviewRepository,
            idempotencyStore = idempotencyStore,
            auditEventPort = auditEventPort,
            clock = clock
        )
    }

    @Bean
    fun reviewQueryService(
        reviewRepository: ReviewRepository
    ): ReviewQueryService {
        return ReviewQueryService(reviewRepository)
    }
}
