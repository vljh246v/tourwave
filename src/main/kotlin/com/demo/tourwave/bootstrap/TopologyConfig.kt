package com.demo.tourwave.bootstrap

import com.demo.tourwave.application.topology.OrganizationAccessGuard
import com.demo.tourwave.application.topology.OrganizationCommandService
import com.demo.tourwave.application.topology.OrganizationMembershipService
import com.demo.tourwave.application.topology.OrganizationQueryService
import com.demo.tourwave.application.topology.port.OrganizationMembershipRepository
import com.demo.tourwave.application.topology.port.OrganizationRepository
import com.demo.tourwave.application.user.port.UserRepository
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock

@Configuration
class TopologyConfig {
    @Bean
    fun organizationAccessGuard(
        organizationRepository: OrganizationRepository,
        membershipRepository: OrganizationMembershipRepository
    ): OrganizationAccessGuard {
        return OrganizationAccessGuard(
            organizationRepository = organizationRepository,
            membershipRepository = membershipRepository
        )
    }

    @Bean
    fun organizationCommandService(
        organizationRepository: OrganizationRepository,
        membershipRepository: OrganizationMembershipRepository,
        userRepository: UserRepository,
        organizationAccessGuard: OrganizationAccessGuard,
        clock: Clock
    ): OrganizationCommandService {
        return OrganizationCommandService(
            organizationRepository = organizationRepository,
            membershipRepository = membershipRepository,
            userRepository = userRepository,
            organizationAccessGuard = organizationAccessGuard,
            clock = clock
        )
    }

    @Bean
    fun organizationMembershipService(
        membershipRepository: OrganizationMembershipRepository,
        userRepository: UserRepository,
        organizationAccessGuard: OrganizationAccessGuard,
        clock: Clock
    ): OrganizationMembershipService {
        return OrganizationMembershipService(
            membershipRepository = membershipRepository,
            userRepository = userRepository,
            organizationAccessGuard = organizationAccessGuard,
            clock = clock
        )
    }

    @Bean
    fun organizationQueryService(
        organizationRepository: OrganizationRepository,
        membershipRepository: OrganizationMembershipRepository,
        organizationAccessGuard: OrganizationAccessGuard
    ): OrganizationQueryService {
        return OrganizationQueryService(
            organizationRepository = organizationRepository,
            membershipRepository = membershipRepository,
            organizationAccessGuard = organizationAccessGuard
        )
    }
}
