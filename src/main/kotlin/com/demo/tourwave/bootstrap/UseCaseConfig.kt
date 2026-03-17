package com.demo.tourwave.bootstrap

import com.demo.tourwave.application.booking.BookingCommandService
import com.demo.tourwave.application.booking.BookingRefundPreviewService
import com.demo.tourwave.application.booking.OfferExpirationService
import com.demo.tourwave.application.booking.PaymentLedgerService
import com.demo.tourwave.application.booking.RefundRetryService
import com.demo.tourwave.application.booking.WaitlistOperatorService
import com.demo.tourwave.application.booking.BookingQueryService
import com.demo.tourwave.application.booking.port.BookingRepository
import com.demo.tourwave.application.booking.port.OccurrenceRepository
import com.demo.tourwave.application.booking.port.PaymentRecordRepository
import com.demo.tourwave.application.booking.port.RefundExecutionPort
import com.demo.tourwave.application.common.TimeWindowPolicyService
import com.demo.tourwave.application.common.IdempotencyPurgeService
import com.demo.tourwave.application.common.port.AuditEventPort
import com.demo.tourwave.application.common.port.IdempotencyMaintenancePort
import com.demo.tourwave.application.common.port.IdempotencyStore
import com.demo.tourwave.application.inquiry.InquiryAccessPolicy
import com.demo.tourwave.application.inquiry.InquiryCommandService
import com.demo.tourwave.application.inquiry.InquiryQueryService
import com.demo.tourwave.application.participant.InvitedParticipantExpirationService
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
import com.demo.tourwave.application.user.port.UserRepository
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock

@Configuration
class UseCaseConfig {
    @Bean
    fun timeWindowPolicyService(): TimeWindowPolicyService = TimeWindowPolicyService()

    @Bean
    fun bookingCommandService(
        bookingRepository: BookingRepository,
        occurrenceRepository: OccurrenceRepository,
        bookingParticipantRepository: BookingParticipantRepository,
        idempotencyStore: IdempotencyStore,
        auditEventPort: AuditEventPort,
        paymentLedgerService: PaymentLedgerService,
        timeWindowPolicyService: TimeWindowPolicyService,
        clock: Clock
    ): BookingCommandService {
        return BookingCommandService(
            bookingRepository = bookingRepository,
            occurrenceRepository = occurrenceRepository,
            bookingParticipantRepository = bookingParticipantRepository,
            idempotencyStore = idempotencyStore,
            auditEventPort = auditEventPort,
            paymentLedgerService = paymentLedgerService,
            timeWindowPolicyService = timeWindowPolicyService,
            clock = clock
        )
    }

    @Bean
    fun paymentLedgerService(
        paymentRecordRepository: PaymentRecordRepository,
        refundExecutionPort: RefundExecutionPort,
        clock: Clock
    ): PaymentLedgerService {
        return PaymentLedgerService(
            paymentRecordRepository = paymentRecordRepository,
            refundExecutionPort = refundExecutionPort,
            clock = clock
        )
    }

    @Bean
    fun refundRetryService(
        paymentRecordRepository: PaymentRecordRepository,
        bookingRepository: BookingRepository,
        refundExecutionPort: RefundExecutionPort,
        auditEventPort: AuditEventPort,
        clock: Clock
    ): RefundRetryService {
        return RefundRetryService(
            paymentRecordRepository = paymentRecordRepository,
            bookingRepository = bookingRepository,
            refundExecutionPort = refundExecutionPort,
            auditEventPort = auditEventPort,
            clock = clock
        )
    }

    @Bean
    fun idempotencyPurgeService(
        idempotencyMaintenancePort: IdempotencyMaintenancePort,
        auditEventPort: AuditEventPort,
        clock: Clock
    ): IdempotencyPurgeService {
        return IdempotencyPurgeService(
            idempotencyMaintenancePort = idempotencyMaintenancePort,
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
    fun bookingRefundPreviewService(
        bookingRepository: BookingRepository,
        occurrenceRepository: OccurrenceRepository,
        participantAccessPolicy: ParticipantAccessPolicy,
        paymentLedgerService: PaymentLedgerService,
        clock: Clock
    ): BookingRefundPreviewService {
        return BookingRefundPreviewService(
            bookingRepository = bookingRepository,
            occurrenceRepository = occurrenceRepository,
            participantAccessPolicy = participantAccessPolicy,
            paymentLedgerService = paymentLedgerService,
            clock = clock
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
        timeWindowPolicyService: TimeWindowPolicyService,
        idempotencyStore: IdempotencyStore,
        auditEventPort: AuditEventPort,
        clock: Clock
    ): ParticipantCommandService {
        return ParticipantCommandService(
            bookingRepository = bookingRepository,
            occurrenceRepository = occurrenceRepository,
            bookingParticipantRepository = bookingParticipantRepository,
            participantInvitationLifecycleService = participantInvitationLifecycleService,
            timeWindowPolicyService = timeWindowPolicyService,
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
        timeWindowPolicyService: TimeWindowPolicyService,
        clock: Clock
    ): ParticipantInvitationLifecycleService {
        return ParticipantInvitationLifecycleService(
            bookingRepository = bookingRepository,
            occurrenceRepository = occurrenceRepository,
            bookingParticipantRepository = bookingParticipantRepository,
            timeWindowPolicyService = timeWindowPolicyService,
            clock = clock
        )
    }

    @Bean
    fun offerExpirationService(
        bookingRepository: BookingRepository,
        occurrenceRepository: OccurrenceRepository,
        auditEventPort: AuditEventPort,
        paymentLedgerService: PaymentLedgerService,
        timeWindowPolicyService: TimeWindowPolicyService,
        clock: Clock
    ): OfferExpirationService {
        return OfferExpirationService(
            bookingRepository = bookingRepository,
            occurrenceRepository = occurrenceRepository,
            auditEventPort = auditEventPort,
            paymentLedgerService = paymentLedgerService,
            timeWindowPolicyService = timeWindowPolicyService,
            clock = clock
        )
    }

    @Bean
    fun invitedParticipantExpirationService(
        participantInvitationLifecycleService: ParticipantInvitationLifecycleService,
        auditEventPort: AuditEventPort,
        clock: Clock
    ): InvitedParticipantExpirationService {
        return InvitedParticipantExpirationService(
            participantInvitationLifecycleService = participantInvitationLifecycleService,
            auditEventPort = auditEventPort,
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
        userRepository: UserRepository
    ): UserCommandService {
        return UserCommandService(userRepository)
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
