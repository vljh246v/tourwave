package com.demo.tourwave.adapter.`in`.web.organization

import com.demo.tourwave.application.common.port.AuthzGuardPort
import com.demo.tourwave.application.organization.ChangeOrganizationMemberRoleCommand
import com.demo.tourwave.application.organization.CreateOrganizationCommand
import com.demo.tourwave.application.organization.DeactivateOrganizationMemberCommand
import com.demo.tourwave.application.organization.InviteOrganizationMemberCommand
import com.demo.tourwave.application.organization.OrganizationCommandService
import com.demo.tourwave.application.organization.OrganizationMembershipService
import com.demo.tourwave.application.organization.OrganizationQueryService
import com.demo.tourwave.application.organization.UpdateOrganizationProfileCommand
import com.demo.tourwave.domain.organization.OrganizationRole
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController

@RestController
class OrganizationOperatorController(
    private val organizationCommandService: OrganizationCommandService,
    private val organizationQueryService: OrganizationQueryService,
    private val organizationMembershipService: OrganizationMembershipService,
    private val authzGuardPort: AuthzGuardPort,
) {
    @PostMapping("/operator/organizations")
    fun createOrganization(
        @RequestHeader("X-Actor-User-Id", required = false) actorUserId: Long?,
        @RequestHeader("Idempotency-Key") idempotencyKey: String,
        @RequestBody request: CreateOrganizationWebRequest,
    ): ResponseEntity<OrganizationOperatorResponse> {
        val actorUserIdRequired = authzGuardPort.requireActorUserId(actorUserId)
        val organization =
            organizationCommandService.createOrganization(
                CreateOrganizationCommand(
                    actorUserId = actorUserIdRequired,
                    slug = request.slug,
                    name = request.name,
                    description = request.description,
                    publicDescription = request.publicDescription,
                    contactEmail = request.contactEmail,
                    contactPhone = request.contactPhone,
                    websiteUrl = request.websiteUrl,
                    businessName = request.businessName,
                    businessRegistrationNumber = request.businessRegistrationNumber,
                    timezone = request.timezone,
                    idempotencyKey = idempotencyKey,
                ),
            )
        return ResponseEntity.status(201).body(organization.toOperatorResponse())
    }

    @GetMapping("/operator/organizations/{organizationId}")
    fun getOperatorOrganization(
        @PathVariable organizationId: Long,
        @RequestHeader("X-Actor-User-Id", required = false) actorUserId: Long?,
    ): ResponseEntity<OrganizationOperatorResponse> {
        val actorUserIdRequired = authzGuardPort.requireActorUserId(actorUserId)
        return ResponseEntity.ok(
            organizationQueryService.getOperatorOrganization(actorUserIdRequired, organizationId).toOperatorResponse(),
        )
    }

    @PatchMapping("/operator/organizations/{organizationId}")
    fun updateOrganization(
        @PathVariable organizationId: Long,
        @RequestHeader("X-Actor-User-Id", required = false) actorUserId: Long?,
        @RequestHeader("Idempotency-Key") idempotencyKey: String,
        @RequestBody request: UpdateOrganizationWebRequest,
    ): ResponseEntity<OrganizationOperatorResponse> {
        val actorUserIdRequired = authzGuardPort.requireActorUserId(actorUserId)
        return ResponseEntity.ok(
            organizationCommandService.updateOrganizationProfile(
                UpdateOrganizationProfileCommand(
                    actorUserId = actorUserIdRequired,
                    organizationId = organizationId,
                    name = request.name,
                    description = request.description,
                    publicDescription = request.publicDescription,
                    contactEmail = request.contactEmail,
                    contactPhone = request.contactPhone,
                    websiteUrl = request.websiteUrl,
                    businessName = request.businessName,
                    businessRegistrationNumber = request.businessRegistrationNumber,
                    timezone = request.timezone,
                    idempotencyKey = idempotencyKey,
                ),
            ).toOperatorResponse(),
        )
    }

    @GetMapping("/operator/organizations/{organizationId}/members")
    fun listMemberships(
        @PathVariable organizationId: Long,
        @RequestHeader("X-Actor-User-Id", required = false) actorUserId: Long?,
    ): ResponseEntity<List<OrganizationMembershipResponse>> {
        val actorUserIdRequired = authzGuardPort.requireActorUserId(actorUserId)
        return ResponseEntity.ok(
            organizationMembershipService.listMemberships(actorUserIdRequired, organizationId).map { it.toResponse() },
        )
    }

    @PostMapping("/operator/organizations/{organizationId}/members/invitations")
    fun inviteMember(
        @PathVariable organizationId: Long,
        @RequestHeader("X-Actor-User-Id", required = false) actorUserId: Long?,
        @RequestHeader("Idempotency-Key") idempotencyKey: String,
        @RequestBody request: InviteOrganizationMemberWebRequest,
    ): ResponseEntity<OrganizationMembershipResponse> {
        val actorUserIdRequired = authzGuardPort.requireActorUserId(actorUserId)
        return ResponseEntity.status(201).body(
            organizationMembershipService.invite(
                InviteOrganizationMemberCommand(
                    actorUserId = actorUserIdRequired,
                    organizationId = organizationId,
                    userId = request.userId,
                    role = OrganizationRole.valueOf(request.role.trim().uppercase()),
                    idempotencyKey = idempotencyKey,
                ),
            ).toResponse(),
        )
    }

    @PatchMapping("/operator/organizations/{organizationId}/members/{memberUserId}/role")
    fun updateMemberRole(
        @PathVariable organizationId: Long,
        @PathVariable memberUserId: Long,
        @RequestHeader("X-Actor-User-Id", required = false) actorUserId: Long?,
        @RequestHeader("Idempotency-Key") idempotencyKey: String,
        @RequestBody request: UpdateOrganizationMemberRoleWebRequest,
    ): ResponseEntity<OrganizationMembershipResponse> {
        val actorUserIdRequired = authzGuardPort.requireActorUserId(actorUserId)
        return ResponseEntity.ok(
            organizationMembershipService.changeRole(
                ChangeOrganizationMemberRoleCommand(
                    actorUserId = actorUserIdRequired,
                    organizationId = organizationId,
                    memberUserId = memberUserId,
                    role = OrganizationRole.valueOf(request.role.trim().uppercase()),
                    idempotencyKey = idempotencyKey,
                ),
            ).toResponse(),
        )
    }

    @PatchMapping("/operator/organizations/{organizationId}/members/{memberUserId}/deactivate")
    fun deactivateMember(
        @PathVariable organizationId: Long,
        @PathVariable memberUserId: Long,
        @RequestHeader("X-Actor-User-Id", required = false) actorUserId: Long?,
        @RequestHeader("Idempotency-Key") idempotencyKey: String,
    ): ResponseEntity<OrganizationMembershipResponse> {
        val actorUserIdRequired = authzGuardPort.requireActorUserId(actorUserId)
        return ResponseEntity.ok(
            organizationMembershipService.deactivate(
                DeactivateOrganizationMemberCommand(
                    actorUserId = actorUserIdRequired,
                    organizationId = organizationId,
                    memberUserId = memberUserId,
                    idempotencyKey = idempotencyKey,
                ),
            ).toResponse(),
        )
    }
}
