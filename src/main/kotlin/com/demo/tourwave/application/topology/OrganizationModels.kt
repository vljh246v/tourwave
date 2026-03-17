package com.demo.tourwave.application.topology

import com.demo.tourwave.domain.organization.OrganizationRole

data class CreateOrganizationCommand(
    val actorUserId: Long,
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

data class UpdateOrganizationProfileCommand(
    val actorUserId: Long,
    val organizationId: Long,
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

data class InviteOrganizationMemberCommand(
    val actorUserId: Long,
    val organizationId: Long,
    val userId: Long,
    val role: OrganizationRole
)

data class AcceptOrganizationInvitationCommand(
    val actorUserId: Long,
    val organizationId: Long
)

data class ChangeOrganizationMemberRoleCommand(
    val actorUserId: Long,
    val organizationId: Long,
    val memberUserId: Long,
    val role: OrganizationRole
)

data class DeactivateOrganizationMemberCommand(
    val actorUserId: Long,
    val organizationId: Long,
    val memberUserId: Long
)
