package com.demo.tourwave.bootstrap

import com.demo.tourwave.application.booking.port.BookingRepository
import com.demo.tourwave.application.booking.port.OccurrenceRepository
import com.demo.tourwave.application.common.TimeWindowPolicyService
import com.demo.tourwave.application.instructor.port.InstructorProfileRepository
import com.demo.tourwave.application.occurrence.CatalogQueryService
import com.demo.tourwave.application.occurrence.OccurrenceCommandService
import com.demo.tourwave.application.organization.OrganizationAccessGuard
import com.demo.tourwave.application.review.port.ReviewRepository
import com.demo.tourwave.application.tour.port.TourRepository
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock

@Configuration
class OccurrenceConfig {
    @Bean
    fun occurrenceCommandService(
        occurrenceRepository: OccurrenceRepository,
        bookingRepository: BookingRepository,
        tourRepository: TourRepository,
        instructorProfileRepository: InstructorProfileRepository,
        organizationAccessGuard: OrganizationAccessGuard,
        clock: Clock,
    ): OccurrenceCommandService =
        OccurrenceCommandService(
            occurrenceRepository = occurrenceRepository,
            bookingRepository = bookingRepository,
            tourRepository = tourRepository,
            instructorProfileRepository = instructorProfileRepository,
            organizationAccessGuard = organizationAccessGuard,
            clock = clock,
        )

    @Bean
    fun catalogQueryService(
        tourRepository: TourRepository,
        occurrenceRepository: OccurrenceRepository,
        bookingRepository: BookingRepository,
        reviewRepository: ReviewRepository,
        timeWindowPolicyService: TimeWindowPolicyService,
    ): CatalogQueryService =
        CatalogQueryService(
            tourRepository = tourRepository,
            occurrenceRepository = occurrenceRepository,
            bookingRepository = bookingRepository,
            reviewRepository = reviewRepository,
            timeWindowPolicyService = timeWindowPolicyService,
        )
}
