package com.demo.tourwave.bootstrap

import com.demo.tourwave.application.organization.OrganizationAccessGuard
import com.demo.tourwave.application.organization.port.OrganizationRepository
import com.demo.tourwave.application.tour.TourCommandService
import com.demo.tourwave.application.tour.TourQueryService
import com.demo.tourwave.application.tour.port.TourRepository
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock

@Configuration
class TourConfig {
    @Bean
    fun tourCommandService(
        tourRepository: TourRepository,
        organizationRepository: OrganizationRepository,
        organizationAccessGuard: OrganizationAccessGuard,
        clock: Clock,
    ): TourCommandService =
        TourCommandService(
            tourRepository = tourRepository,
            organizationRepository = organizationRepository,
            organizationAccessGuard = organizationAccessGuard,
            clock = clock,
        )

    @Bean
    fun tourQueryService(
        tourRepository: TourRepository,
        organizationAccessGuard: OrganizationAccessGuard,
    ): TourQueryService =
        TourQueryService(
            tourRepository = tourRepository,
            organizationAccessGuard = organizationAccessGuard,
        )
}
