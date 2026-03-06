package com.demo.tourwave.bootstrap

import com.demo.tourwave.application.booking.BookingCommandService
import com.demo.tourwave.application.booking.port.BookingRepository
import com.demo.tourwave.application.booking.port.OccurrenceRepository
import com.demo.tourwave.application.common.port.AuditEventPort
import com.demo.tourwave.application.common.port.IdempotencyStore
import com.demo.tourwave.application.inquiry.InquiryAccessPolicy
import com.demo.tourwave.application.inquiry.InquiryCommandService
import com.demo.tourwave.application.inquiry.InquiryQueryService
import com.demo.tourwave.application.inquiry.port.InquiryRepository
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
        idempotencyStore: IdempotencyStore,
        auditEventPort: AuditEventPort,
        clock: Clock
    ): BookingCommandService {
        return BookingCommandService(
            bookingRepository = bookingRepository,
            occurrenceRepository = occurrenceRepository,
            idempotencyStore = idempotencyStore,
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
        reviewRepository: ReviewRepository,
        idempotencyStore: IdempotencyStore,
        auditEventPort: AuditEventPort,
        clock: Clock
    ): ReviewCommandService {
        return ReviewCommandService(
            bookingRepository = bookingRepository,
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
