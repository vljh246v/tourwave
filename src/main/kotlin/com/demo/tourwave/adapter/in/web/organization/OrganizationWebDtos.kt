package com.demo.tourwave.adapter.`in`.web.organization

import com.demo.tourwave.domain.organization.Organization
import com.demo.tourwave.domain.organization.OrganizationMembership

data class CreateOrganizationWebRequest(
    val slug: String,
    val name: String,
    val description: String? = null,
    val publicDescription: String? = null,
    val contactEmail: String? = null,
    val contactPhone: String? = null,
    val websiteUrl: String? = null,
    val businessName: String? = null,
    val businessRegistrationNumber: String? = null,
    val timezone: String
)

data class UpdateOrganizationWebRequest(
    val name: String,
    val description: String? = null,
    val publicDescription: String? = null,
    val contactEmail: String? = null,
    val contactPhone: String? = null,
    val websiteUrl: String? = null,
    val businessName: String? = null,
    val businessRegistrationNumber: String? = null,
    val timezone: String
)

data class InviteOrganizationMemberWebRequest(
    val userId: Long,
    val role: String
)

data class UpdateOrganizationMemberRoleWebRequest(
    val role: String
)

data class OrganizationOperatorResponse(
    val id: Long,
    val slug: String,
    val name: String,
    val description: String?,
    val publicDescription: String?,
    val contactEmail: String?,
    val contactPhone: String?,
    val websiteUrl: String?,
    val businessName: String?,
    val businessRegistrationNumber: String?,
    val attachmentAssetIds: List<Long>,
    val timezone: String,
    val status: String
)

data class OrganizationPublicResponse(
    val id: Long,
    val slug: String,
    val name: String,
    val publicDescription: String?,
    val contactEmail: String?,
    val contactPhone: String?,
    val websiteUrl: String?,
    val attachmentAssetIds: List<Long>,
    val timezone: String
)

data class OrganizationMembershipResponse(
    val organizationId: Long,
    val userId: Long,
    val role: String,
    val status: String
)

fun Organization.toOperatorResponse(): OrganizationOperatorResponse =
    OrganizationOperatorResponse(
        id = requireNotNull(id),
        slug = slug,
        name = name,
        description = description,
        publicDescription = publicDescription,
        contactEmail = contactEmail,
        contactPhone = contactPhone,
        websiteUrl = websiteUrl,
        businessName = businessName,
        businessRegistrationNumber = businessRegistrationNumber,
        attachmentAssetIds = attachmentAssetIds,
        timezone = timezone,
        status = status.name
    )

fun Organization.toPublicResponse(): OrganizationPublicResponse =
    OrganizationPublicResponse(
        id = requireNotNull(id),
        slug = slug,
        name = name,
        publicDescription = publicDescription,
        contactEmail = contactEmail,
        contactPhone = contactPhone,
        websiteUrl = websiteUrl,
        attachmentAssetIds = attachmentAssetIds,
        timezone = timezone
    )

fun OrganizationMembership.toResponse(): OrganizationMembershipResponse =
    OrganizationMembershipResponse(
        organizationId = organizationId,
        userId = userId,
        role = role.name,
        status = status.name
    )
