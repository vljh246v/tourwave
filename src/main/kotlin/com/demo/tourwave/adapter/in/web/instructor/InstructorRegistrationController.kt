package com.demo.tourwave.adapter.`in`.web.instructor

import com.demo.tourwave.application.common.port.AuthzGuardPort
import com.demo.tourwave.application.topology.ApplyInstructorRegistrationCommand
import com.demo.tourwave.application.topology.InstructorRegistrationService
import com.demo.tourwave.application.topology.ReviewInstructorRegistrationCommand
import com.demo.tourwave.application.user.port.UserRepository
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController

@RestController
class InstructorRegistrationController(
    private val instructorRegistrationService: InstructorRegistrationService,
    private val userRepository: UserRepository,
    private val authzGuardPort: AuthzGuardPort
) {
    @PostMapping("/instructor-registrations")
    fun apply(
        @RequestHeader("X-Actor-User-Id", required = false) actorUserId: Long?,
        @RequestBody request: ApplyInstructorRegistrationWebRequest
    ): ResponseEntity<InstructorRegistrationResponse> {
        val requiredActorUserId = authzGuardPort.requireActorUserId(actorUserId)
        val registration = instructorRegistrationService.apply(
            ApplyInstructorRegistrationCommand(
                actorUserId = requiredActorUserId,
                organizationId = request.organizationId,
                headline = request.headline,
                bio = request.bio,
                languages = request.languages,
                specialties = request.specialties
            )
        )
        return ResponseEntity.status(201).body(registration.toResponse(requireUser(registration.userId)))
    }

    @GetMapping("/organizations/{organizationId}/instructor-registrations")
    fun listByOrganization(
        @PathVariable organizationId: Long,
        @RequestHeader("X-Actor-User-Id", required = false) actorUserId: Long?
    ): ResponseEntity<List<InstructorRegistrationResponse>> {
        val requiredActorUserId = authzGuardPort.requireActorUserId(actorUserId)
        return ResponseEntity.ok(
            instructorRegistrationService.listByOrganization(requiredActorUserId, organizationId).map {
                it.toResponse(requireUser(it.userId))
            }
        )
    }

    @PostMapping("/instructor-registrations/{registrationId}/approve")
    fun approve(
        @PathVariable registrationId: Long,
        @RequestHeader("X-Actor-User-Id", required = false) actorUserId: Long?
    ): ResponseEntity<InstructorRegistrationResponse> {
        val requiredActorUserId = authzGuardPort.requireActorUserId(actorUserId)
        val registration = instructorRegistrationService.approve(
            ReviewInstructorRegistrationCommand(
                actorUserId = requiredActorUserId,
                registrationId = registrationId
            )
        )
        return ResponseEntity.ok(registration.toResponse(requireUser(registration.userId)))
    }

    @PostMapping("/instructor-registrations/{registrationId}/reject")
    fun reject(
        @PathVariable registrationId: Long,
        @RequestHeader("X-Actor-User-Id", required = false) actorUserId: Long?,
        @RequestBody request: RejectInstructorRegistrationWebRequest
    ): ResponseEntity<InstructorRegistrationResponse> {
        val requiredActorUserId = authzGuardPort.requireActorUserId(actorUserId)
        val registration = instructorRegistrationService.reject(
            ReviewInstructorRegistrationCommand(
                actorUserId = requiredActorUserId,
                registrationId = registrationId,
                rejectionReason = request.rejectionReason
            )
        )
        return ResponseEntity.ok(registration.toResponse(requireUser(registration.userId)))
    }

    private fun requireUser(userId: Long) = requireNotNull(userRepository.findById(userId)) {
        "Expected user $userId to exist"
    }
}
