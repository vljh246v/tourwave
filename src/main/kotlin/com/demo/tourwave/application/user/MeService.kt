package com.demo.tourwave.application.user

import com.demo.tourwave.application.organization.OrganizationQueryService
import com.demo.tourwave.application.user.port.UserRepository
import com.demo.tourwave.domain.common.DomainException
import com.demo.tourwave.domain.common.ErrorCode
import com.demo.tourwave.domain.organization.OrganizationMembership
import com.demo.tourwave.domain.user.User

class MeService(
    private val userRepository: UserRepository,
    private val organizationQueryService: OrganizationQueryService,
) {
    fun getCurrentUser(userId: Long): User {
        return userRepository.findById(userId) ?: throw DomainException(
            errorCode = ErrorCode.UNAUTHORIZED,
            status = 401,
            message = "authenticated user does not exist",
        )
    }

    fun getCurrentUserMemberships(userId: Long): List<OrganizationMembership> {
        getCurrentUser(userId)
        return organizationQueryService.getMembershipsForUser(userId)
    }
}
