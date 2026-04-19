package com.demo.tourwave.adapter.`in`.web.organization

import com.demo.tourwave.application.common.port.AuthzGuardPort
import com.demo.tourwave.application.organization.AcceptOrganizationInvitationCommand
import com.demo.tourwave.application.organization.OrganizationMembershipService
import com.demo.tourwave.application.organization.OrganizationQueryService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController

@RestController
class OrganizationPublicController(
    private val organizationQueryService: OrganizationQueryService,
    private val organizationMembershipService: OrganizationMembershipService,
    private val authzGuardPort: AuthzGuardPort,
) {
    @GetMapping("/organizations/{organizationId}")
    fun getPublicOrganization(
        @PathVariable organizationId: Long,
    ): ResponseEntity<OrganizationPublicResponse> {
        return ResponseEntity.ok(
            organizationQueryService.getPublicOrganization(organizationId).toPublicResponse(),
        )
    }

    @PostMapping("/organizations/{organizationId}/memberships/accept")
    fun acceptInvitation(
        @PathVariable organizationId: Long,
        @RequestHeader("X-Actor-User-Id", required = false) actorUserId: Long?,
        @RequestBody(required = false) request: AcceptOrganizationInvitationWebRequest?,
    ): ResponseEntity<OrganizationMembershipResponse> {
        val actorUserIdRequired = authzGuardPort.requireActorUserId(actorUserId)
        return ResponseEntity.ok(
            organizationMembershipService.acceptInvitation(
                AcceptOrganizationInvitationCommand(
                    actorUserId = actorUserIdRequired,
                    organizationId = organizationId,
                    token = request?.token,
                ),
            ).toResponse(),
        )
    }
}
