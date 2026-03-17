package com.demo.tourwave.application.topology

import com.demo.tourwave.application.topology.port.OrganizationMembershipRepository
import com.demo.tourwave.application.topology.port.OrganizationRepository
import com.demo.tourwave.application.user.port.UserRepository
import com.demo.tourwave.domain.common.DomainException
import com.demo.tourwave.domain.common.ErrorCode
import com.demo.tourwave.domain.organization.Organization
import com.demo.tourwave.domain.organization.OrganizationMembership
import com.demo.tourwave.domain.organization.OrganizationRole
import java.time.Clock

class OrganizationCommandService(
    private val organizationRepository: OrganizationRepository,
    private val membershipRepository: OrganizationMembershipRepository,
    private val userRepository: UserRepository,
    private val organizationAccessGuard: OrganizationAccessGuard,
    private val clock: Clock
) {
    fun createOrganization(command: CreateOrganizationCommand): Organization {
        val actor = userRepository.findById(command.actorUserId) ?: throw unauthorized()
        val normalizedSlug = requireValidOrganizationSlug(command.slug)
        if (organizationRepository.findBySlug(normalizedSlug) != null) {
            throw DomainException(
                errorCode = ErrorCode.VALIDATION_ERROR,
                status = 409,
                message = "organization slug already exists"
            )
        }

        val now = clock.instant()
        val organization = organizationRepository.save(
            Organization.create(
                slug = normalizedSlug,
                name = requireValidOrganizationName(command.name),
                description = normalizeOptionalText(command.description, 4000, "description"),
                publicDescription = normalizeOptionalText(command.publicDescription, 4000, "public description"),
                contactEmail = normalizeOptionalEmail(command.contactEmail),
                contactPhone = normalizeOptionalPhone(command.contactPhone),
                websiteUrl = normalizeOptionalUrl(command.websiteUrl),
                businessName = normalizeOptionalText(command.businessName, 255, "business name"),
                businessRegistrationNumber = normalizeOptionalText(command.businessRegistrationNumber, 128, "business registration number"),
                timezone = requireValidTimezone(command.timezone),
                now = now
            )
        )
        membershipRepository.save(
            OrganizationMembership.active(
                organizationId = requireNotNull(organization.id),
                userId = requireNotNull(actor.id),
                role = OrganizationRole.OWNER,
                now = now
            )
        )
        return organization
    }

    fun updateOrganizationProfile(command: UpdateOrganizationProfileCommand): Organization {
        val organization = organizationAccessGuard.requireOperator(command.actorUserId, command.organizationId)
            .let { organizationRepository.findById(command.organizationId)!! }

        return organizationRepository.save(
            organization.updateProfile(
                name = requireValidOrganizationName(command.name),
                description = normalizeOptionalText(command.description, 4000, "description"),
                publicDescription = normalizeOptionalText(command.publicDescription, 4000, "public description"),
                contactEmail = normalizeOptionalEmail(command.contactEmail),
                contactPhone = normalizeOptionalPhone(command.contactPhone),
                websiteUrl = normalizeOptionalUrl(command.websiteUrl),
                businessName = normalizeOptionalText(command.businessName, 255, "business name"),
                businessRegistrationNumber = normalizeOptionalText(command.businessRegistrationNumber, 128, "business registration number"),
                timezone = requireValidTimezone(command.timezone),
                now = clock.instant()
            )
        )
    }

    private fun unauthorized(): DomainException {
        return DomainException(
            errorCode = ErrorCode.UNAUTHORIZED,
            status = 401,
            message = "authenticated user does not exist"
        )
    }
}
