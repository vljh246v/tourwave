package com.demo.tourwave.application.organization

import com.demo.tourwave.application.common.port.AuditEventCommand
import com.demo.tourwave.application.common.port.AuditEventPort
import com.demo.tourwave.application.organization.port.OrganizationMembershipRepository
import com.demo.tourwave.application.organization.port.OrganizationRepository
import com.demo.tourwave.application.user.port.UserRepository
import com.demo.tourwave.domain.common.DomainException
import com.demo.tourwave.domain.common.ErrorCode
import com.demo.tourwave.domain.organization.Organization
import com.demo.tourwave.domain.organization.OrganizationMembership
import com.demo.tourwave.domain.organization.OrganizationRole
import org.springframework.transaction.annotation.Transactional
import java.time.Clock

@Transactional
class OrganizationCommandService(
    private val organizationRepository: OrganizationRepository,
    private val membershipRepository: OrganizationMembershipRepository,
    private val userRepository: UserRepository,
    private val organizationAccessGuard: OrganizationAccessGuard,
    private val auditEventPort: AuditEventPort,
    private val clock: Clock,
) {
    fun createOrganization(command: CreateOrganizationCommand): Organization {
        val actor = userRepository.findById(command.actorUserId) ?: throw unauthorized()
        val normalizedSlug = requireValidOrganizationSlug(command.slug)
        if (organizationRepository.findBySlug(normalizedSlug) != null) {
            throw DomainException(
                errorCode = ErrorCode.VALIDATION_ERROR,
                status = 409,
                message = "organization slug already exists",
            )
        }

        val now = clock.instant()
        val organization =
            organizationRepository.save(
                Organization.create(
                    slug = normalizedSlug,
                    name = requireValidOrganizationName(command.name),
                    description = normalizeOptionalText(command.description, 4000, "description"),
                    publicDescription = normalizeOptionalText(command.publicDescription, 4000, "public description"),
                    contactEmail = normalizeOptionalEmail(command.contactEmail),
                    contactPhone = normalizeOptionalPhone(command.contactPhone),
                    websiteUrl = normalizeOptionalUrl(command.websiteUrl),
                    businessName = normalizeOptionalText(command.businessName, 255, "business name"),
                    businessRegistrationNumber =
                        normalizeOptionalText(
                            command.businessRegistrationNumber,
                            128,
                            "business registration number",
                        ),
                    timezone = requireValidTimezone(command.timezone),
                    now = now,
                ),
            )
        membershipRepository.save(
            OrganizationMembership.active(
                organizationId = requireNotNull(organization.id),
                userId = requireNotNull(actor.id),
                role = OrganizationRole.OWNER,
                now = now,
            ),
        )
        auditEventPort.append(
            AuditEventCommand(
                actor = "USER:${command.actorUserId}",
                action = "ORGANIZATION_CREATED",
                resourceType = "ORGANIZATION",
                resourceId = requireNotNull(organization.id),
                occurredAtUtc = clock.instant(),
                reasonCode = "ORGANIZATION_CREATED",
                afterJson = organizationSnapshot(organization),
            ),
        )
        return organization
    }

    fun updateOrganizationProfile(command: UpdateOrganizationProfileCommand): Organization {
        organizationAccessGuard.requireOperator(command.actorUserId, command.organizationId)
        val organization =
            organizationRepository.findById(command.organizationId)
                ?: throw DomainException(
                    errorCode = ErrorCode.VALIDATION_ERROR,
                    status = 404,
                    message = "organization ${command.organizationId} not found",
                )

        val saved =
            organizationRepository.save(
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
                    now = clock.instant(),
                ),
            )
        auditEventPort.append(
            AuditEventCommand(
                actor = "OPERATOR:${command.actorUserId}",
                action = "ORGANIZATION_PROFILE_UPDATED",
                resourceType = "ORGANIZATION",
                resourceId = requireNotNull(saved.id),
                occurredAtUtc = clock.instant(),
                reasonCode = "ORGANIZATION_PROFILE_UPDATED",
                beforeJson = organizationSnapshot(organization),
                afterJson = organizationSnapshot(saved),
            ),
        )
        return saved
    }

    private fun organizationSnapshot(org: Organization): Map<String, Any?> =
        mapOf(
            "slug" to org.slug,
            "name" to org.name,
            "timezone" to org.timezone,
        )

    private fun unauthorized(): DomainException {
        return DomainException(
            errorCode = ErrorCode.UNAUTHORIZED,
            status = 401,
            message = "authenticated user does not exist",
        )
    }
}
