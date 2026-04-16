package com.demo.tourwave.bootstrap

import com.demo.tourwave.application.instructor.InstructorProfileService
import com.demo.tourwave.application.instructor.InstructorRegistrationService
import com.demo.tourwave.application.instructor.port.InstructorProfileRepository
import com.demo.tourwave.application.instructor.port.InstructorRegistrationRepository
import com.demo.tourwave.application.organization.OrganizationAccessGuard
import com.demo.tourwave.application.organization.port.OrganizationRepository
import com.demo.tourwave.application.user.port.UserRepository
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock

@Configuration
class InstructorConfig {
    @Bean
    fun instructorRegistrationService(
        registrationRepository: InstructorRegistrationRepository,
        instructorProfileRepository: InstructorProfileRepository,
        organizationRepository: OrganizationRepository,
        organizationAccessGuard: OrganizationAccessGuard,
        userRepository: UserRepository,
        clock: Clock
    ): InstructorRegistrationService = InstructorRegistrationService(
        registrationRepository = registrationRepository,
        instructorProfileRepository = instructorProfileRepository,
        organizationRepository = organizationRepository,
        organizationAccessGuard = organizationAccessGuard,
        userRepository = userRepository,
        clock = clock
    )

    @Bean
    fun instructorProfileService(
        instructorProfileRepository: InstructorProfileRepository,
        instructorRegistrationRepository: InstructorRegistrationRepository,
        userRepository: UserRepository,
        clock: Clock
    ): InstructorProfileService = InstructorProfileService(
        instructorProfileRepository = instructorProfileRepository,
        instructorRegistrationRepository = instructorRegistrationRepository,
        userRepository = userRepository,
        clock = clock
    )
}
