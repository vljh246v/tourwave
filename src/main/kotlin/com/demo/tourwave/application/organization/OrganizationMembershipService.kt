package com.demo.tourwave.application.organization

import com.demo.tourwave.application.common.port.AuditEventCommand
import com.demo.tourwave.application.common.port.AuditEventPort
import com.demo.tourwave.application.common.port.IdempotencyDecision
import com.demo.tourwave.application.common.port.IdempotencyStore
import com.demo.tourwave.application.organization.port.OrganizationMembershipRepository
import com.demo.tourwave.application.user.port.UserRepository
import com.demo.tourwave.domain.common.DomainException
import com.demo.tourwave.domain.common.ErrorCode
import com.demo.tourwave.domain.organization.OrganizationMembership
import com.demo.tourwave.domain.organization.OrganizationMembershipStatus
import com.demo.tourwave.domain.organization.OrganizationRole
import org.springframework.transaction.annotation.Transactional
import java.security.MessageDigest
import java.time.Clock

@Transactional
class OrganizationMembershipService(
    private val membershipRepository: OrganizationMembershipRepository,
    private val userRepository: UserRepository,
    private val organizationAccessGuard: OrganizationAccessGuard,
    private val organizationInvitationDeliveryService: OrganizationInvitationDeliveryService,
    private val auditEventPort: AuditEventPort,
    private val idempotencyStore: IdempotencyStore,
    private val clock: Clock,
) {
    fun invite(command: InviteOrganizationMemberCommand): OrganizationMembership {
        val pathTemplate = "/operator/organizations/{organizationId}/members/invitations"
        val requestHash = requestHash("${command.organizationId}|${command.userId}|${command.role}")

        return when (
            val decision =
                idempotencyStore.reserveOrReplay(
                    actorUserId = command.actorUserId,
                    method = "POST",
                    pathTemplate = pathTemplate,
                    idempotencyKey = command.idempotencyKey,
                    requestHash = requestHash,
                )
        ) {
            is IdempotencyDecision.Replay -> decision.body as OrganizationMembership
            IdempotencyDecision.Reserved -> {
                val actorMembership = organizationAccessGuard.requireOperator(command.actorUserId, command.organizationId)
                val targetUser =
                    userRepository.findById(command.userId) ?: throw DomainException(
                        errorCode = ErrorCode.VALIDATION_ERROR,
                        status = 404,
                        message = "user ${command.userId} not found",
                    )
                validateRoleAssignment(
                    actorRole = actorMembership.role,
                    currentRole = membershipRepository.findByOrganizationIdAndUserId(command.organizationId, command.userId)?.role,
                    requestedRole = command.role,
                    targetUserId = requireNotNull(targetUser.id),
                    actorUserId = command.actorUserId,
                )

                val now = clock.instant()
                val existing = membershipRepository.findByOrganizationIdAndUserId(command.organizationId, command.userId)
                val saved =
                    when {
                        existing == null ->
                            OrganizationMembership.invite(
                                organizationId = command.organizationId,
                                userId = requireNotNull(targetUser.id),
                                role = command.role,
                                now = now,
                            )
                        existing.status == OrganizationMembershipStatus.INACTIVE || existing.status == OrganizationMembershipStatus.INVITED ->
                            existing.changeRole(command.role, now).copy(status = OrganizationMembershipStatus.INVITED, updatedAt = now)
                        else -> throw DomainException(
                            errorCode = ErrorCode.VALIDATION_ERROR,
                            status = 409,
                            message = "organization membership already active",
                        )
                    }
                val result =
                    membershipRepository.save(saved).also {
                        organizationInvitationDeliveryService.sendInvitation(it)
                    }
                auditEventPort.append(
                    AuditEventCommand(
                        actor = "OPERATOR:${command.actorUserId}",
                        action = "ORGANIZATION_MEMBER_INVITED",
                        resourceType = "ORGANIZATION_MEMBERSHIP",
                        resourceId = requireNotNull(targetUser.id),
                        occurredAtUtc = clock.instant(),
                        reasonCode = "ORGANIZATION_MEMBER_INVITED",
                        afterJson = membershipSnapshot(result),
                    ),
                )
                idempotencyStore.complete(
                    actorUserId = command.actorUserId,
                    method = "POST",
                    pathTemplate = pathTemplate,
                    idempotencyKey = command.idempotencyKey,
                    status = 201,
                    body = result,
                )
                result
            }
        }
    }

    fun acceptInvitation(command: AcceptOrganizationInvitationCommand): OrganizationMembership {
        command.token?.trim()?.takeIf { it.isNotBlank() }?.let {
            organizationInvitationDeliveryService.consumeInvitationToken(command.actorUserId, it)
        }
        val membership =
            membershipRepository.findByOrganizationIdAndUserId(command.organizationId, command.actorUserId)
                ?: throw DomainException(
                    errorCode = ErrorCode.FORBIDDEN,
                    status = 403,
                    message = "organization invitation does not exist",
                )
        if (membership.status != OrganizationMembershipStatus.INVITED) {
            throw DomainException(
                errorCode = ErrorCode.VALIDATION_ERROR,
                status = 409,
                message = "organization invitation is not pending",
            )
        }
        return membershipRepository.save(membership.activate(clock.instant()))
    }

    fun changeRole(command: ChangeOrganizationMemberRoleCommand): OrganizationMembership {
        val pathTemplate = "/operator/organizations/{organizationId}/members/{memberUserId}/role"
        val requestHash = requestHash("${command.organizationId}|${command.memberUserId}|${command.role}")

        return when (
            val decision =
                idempotencyStore.reserveOrReplay(
                    actorUserId = command.actorUserId,
                    method = "PATCH",
                    pathTemplate = pathTemplate,
                    idempotencyKey = command.idempotencyKey,
                    requestHash = requestHash,
                )
        ) {
            is IdempotencyDecision.Replay -> decision.body as OrganizationMembership
            IdempotencyDecision.Reserved -> {
                val actorMembership = organizationAccessGuard.requireOperator(command.actorUserId, command.organizationId)
                val membership =
                    membershipRepository.findByOrganizationIdAndUserId(command.organizationId, command.memberUserId)
                        ?: throw DomainException(
                            errorCode = ErrorCode.VALIDATION_ERROR,
                            status = 404,
                            message = "organization membership not found",
                        )
                validateRoleAssignment(
                    actorRole = actorMembership.role,
                    currentRole = membership.role,
                    requestedRole = command.role,
                    targetUserId = command.memberUserId,
                    actorUserId = command.actorUserId,
                )
                val saved = membershipRepository.save(membership.changeRole(command.role, clock.instant()))
                auditEventPort.append(
                    AuditEventCommand(
                        actor = "OPERATOR:${command.actorUserId}",
                        action = "ORGANIZATION_MEMBER_ROLE_CHANGED",
                        resourceType = "ORGANIZATION_MEMBERSHIP",
                        resourceId = command.memberUserId,
                        occurredAtUtc = clock.instant(),
                        reasonCode = "ORGANIZATION_MEMBER_ROLE_CHANGED",
                        beforeJson = membershipSnapshot(membership),
                        afterJson = membershipSnapshot(saved),
                    ),
                )
                idempotencyStore.complete(
                    actorUserId = command.actorUserId,
                    method = "PATCH",
                    pathTemplate = pathTemplate,
                    idempotencyKey = command.idempotencyKey,
                    status = 200,
                    body = saved,
                )
                saved
            }
        }
    }

    fun deactivate(command: DeactivateOrganizationMemberCommand): OrganizationMembership {
        val pathTemplate = "/operator/organizations/{organizationId}/members/{memberUserId}/deactivate"
        val requestHash = requestHash("${command.organizationId}|${command.memberUserId}")

        return when (
            val decision =
                idempotencyStore.reserveOrReplay(
                    actorUserId = command.actorUserId,
                    method = "PATCH",
                    pathTemplate = pathTemplate,
                    idempotencyKey = command.idempotencyKey,
                    requestHash = requestHash,
                )
        ) {
            is IdempotencyDecision.Replay -> decision.body as OrganizationMembership
            IdempotencyDecision.Reserved -> {
                val actorMembership = organizationAccessGuard.requireOperator(command.actorUserId, command.organizationId)
                val membership =
                    membershipRepository.findByOrganizationIdAndUserId(command.organizationId, command.memberUserId)
                        ?: throw DomainException(
                            errorCode = ErrorCode.VALIDATION_ERROR,
                            status = 404,
                            message = "organization membership not found",
                        )
                if (command.memberUserId == command.actorUserId) {
                    throw DomainException(
                        errorCode = ErrorCode.VALIDATION_ERROR,
                        status = 409,
                        message = "operator cannot deactivate own membership",
                    )
                }
                if (membership.role == OrganizationRole.OWNER && actorMembership.role != OrganizationRole.OWNER) {
                    throw DomainException(
                        errorCode = ErrorCode.FORBIDDEN,
                        status = 403,
                        message = "only owner can manage owner membership",
                    )
                }
                val saved = membershipRepository.save(membership.deactivate(clock.instant()))
                auditEventPort.append(
                    AuditEventCommand(
                        actor = "OPERATOR:${command.actorUserId}",
                        action = "ORGANIZATION_MEMBER_DEACTIVATED",
                        resourceType = "ORGANIZATION_MEMBERSHIP",
                        resourceId = command.memberUserId,
                        occurredAtUtc = clock.instant(),
                        reasonCode = "ORGANIZATION_MEMBER_DEACTIVATED",
                        beforeJson = membershipSnapshot(membership),
                        afterJson = membershipSnapshot(saved),
                    ),
                )
                idempotencyStore.complete(
                    actorUserId = command.actorUserId,
                    method = "PATCH",
                    pathTemplate = pathTemplate,
                    idempotencyKey = command.idempotencyKey,
                    status = 200,
                    body = saved,
                )
                saved
            }
        }
    }

    fun listMemberships(
        actorUserId: Long,
        organizationId: Long,
    ): List<OrganizationMembership> {
        organizationAccessGuard.requireOperator(actorUserId, organizationId)
        return membershipRepository.findByOrganizationId(organizationId)
    }

    private fun membershipSnapshot(membership: OrganizationMembership): Map<String, Any?> =
        mapOf(
            "organizationId" to membership.organizationId,
            "userId" to membership.userId,
            "role" to membership.role.name,
            "status" to membership.status.name,
        )

    private fun requestHash(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(input.toByteArray()).joinToString("") { "%02x".format(it) }
    }

    private fun validateRoleAssignment(
        actorRole: OrganizationRole,
        currentRole: OrganizationRole?,
        requestedRole: OrganizationRole,
        targetUserId: Long,
        actorUserId: Long,
    ) {
        if (requestedRole == OrganizationRole.OWNER && actorRole != OrganizationRole.OWNER) {
            throw DomainException(
                errorCode = ErrorCode.FORBIDDEN,
                status = 403,
                message = "only owner can assign owner role",
            )
        }
        if (currentRole == OrganizationRole.OWNER && actorRole != OrganizationRole.OWNER) {
            throw DomainException(
                errorCode = ErrorCode.FORBIDDEN,
                status = 403,
                message = "only owner can manage owner membership",
            )
        }
        if (actorRole == OrganizationRole.ADMIN && requestedRole == OrganizationRole.ADMIN && targetUserId == actorUserId) {
            return
        }
    }
}
