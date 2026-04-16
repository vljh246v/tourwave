package com.demo.tourwave.adapter.`in`.web.auth

import com.demo.tourwave.application.common.port.AuthzGuardPort
import com.demo.tourwave.application.user.MeService
import com.demo.tourwave.domain.organization.OrganizationMembership
import com.demo.tourwave.domain.user.User
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
class MeController(
    private val meService: MeService,
    private val authCommandService: com.demo.tourwave.application.auth.AuthCommandService,
    private val authzGuardPort: AuthzGuardPort,
) {
    @GetMapping("/me")
    fun getMe(
        @RequestHeader("X-Actor-User-Id", required = false) actorUserId: Long?,
    ): ResponseEntity<MeResponse> {
        val requiredActorUserId = authzGuardPort.requireActorUserId(actorUserId)
        return ResponseEntity.ok(
            meService.getCurrentUser(requiredActorUserId).toMeResponse(
                meService.getCurrentUserMemberships(requiredActorUserId),
            ),
        )
    }

    @PatchMapping("/me")
    fun updateMe(
        @RequestHeader("X-Actor-User-Id", required = false) actorUserId: Long?,
        @RequestBody request: MeUpdateRequest,
    ): ResponseEntity<UserResponse> {
        val requiredActorUserId = authzGuardPort.requireActorUserId(actorUserId)
        return ResponseEntity.ok(
            meService.updateCurrentUser(
                userId = requiredActorUserId,
                displayName = request.displayName,
            ).toUserResponse(),
        )
    }

    @PostMapping("/me/deactivate")
    fun deactivateMe(
        @RequestHeader("X-Actor-User-Id", required = false) actorUserId: Long?,
    ): ResponseEntity<Void> {
        authCommandService.deactivate(authzGuardPort.requireActorUserId(actorUserId))
        return ResponseEntity.noContent().build()
    }
}

data class MeUpdateRequest(
    val displayName: String,
)

data class MeResponse(
    val user: UserResponse,
    val memberships: List<MembershipResponse> = emptyList(),
)

data class UserResponse(
    val id: Long,
    val email: String,
    val displayName: String,
    val status: String,
    val createdAt: Instant,
)

data class MembershipResponse(
    val organizationId: Long,
    val roles: List<String>,
    val status: String,
)

private fun User.toUserResponse(): UserResponse =
    UserResponse(
        id = requireNotNull(id),
        email = email,
        displayName = displayName,
        status = status.name,
        createdAt = createdAt,
    )

private fun User.toMeResponse(memberships: List<OrganizationMembership>): MeResponse =
    MeResponse(
        user = toUserResponse(),
        memberships =
            memberships.map {
                MembershipResponse(
                    organizationId = it.organizationId,
                    roles = listOf(it.role.name),
                    status = it.status.name,
                )
            },
    )
