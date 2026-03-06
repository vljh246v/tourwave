package com.demo.tourwave.bootstrap

import com.demo.tourwave.application.booking.BookingCommandService
import com.demo.tourwave.application.booking.port.BookingRepository
import com.demo.tourwave.application.booking.port.OccurrenceRepository
import com.demo.tourwave.application.common.port.IdempotencyStore
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
        clock: Clock
    ): BookingCommandService {
        return BookingCommandService(
            bookingRepository = bookingRepository,
            occurrenceRepository = occurrenceRepository,
            idempotencyStore = idempotencyStore,
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
