package com.demo.tourwave.bootstrap

import com.demo.tourwave.application.booking.BookingCommandService
import com.demo.tourwave.application.booking.port.BookingRepository
import com.demo.tourwave.application.booking.port.OccurrenceRepository
import com.demo.tourwave.application.common.port.AuditEventPort
import com.demo.tourwave.application.common.port.IdempotencyStore
import com.demo.tourwave.application.inquiry.InquiryCommandService
import com.demo.tourwave.application.inquiry.port.InquiryRepository
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
        idempotencyStore: IdempotencyStore,
        auditEventPort: AuditEventPort,
        clock: Clock
    ): InquiryCommandService {
        return InquiryCommandService(
            bookingRepository = bookingRepository,
            inquiryRepository = inquiryRepository,
            idempotencyStore = idempotencyStore,
            auditEventPort = auditEventPort,
            clock = clock
        )
    }

    @Bean
    fun userCommandService(
        userQueryPort: UserQueryPort
    ): UserCommandService {
        return UserCommandService(userQueryPort)
    }
}
