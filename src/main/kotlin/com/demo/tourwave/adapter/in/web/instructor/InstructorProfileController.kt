package com.demo.tourwave.adapter.`in`.web.instructor

import com.demo.tourwave.application.common.port.AuthzGuardPort
import com.demo.tourwave.application.instructor.InstructorProfileService
import com.demo.tourwave.application.instructor.UpsertInstructorProfileCommand
import com.demo.tourwave.application.user.port.UserRepository
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class InstructorProfileController(
    private val instructorProfileService: InstructorProfileService,
    private val userRepository: UserRepository,
    private val authzGuardPort: AuthzGuardPort
) {
    @GetMapping("/me/instructor-profile")
    fun getMyProfile(
        @RequestHeader("X-Actor-User-Id", required = false) actorUserId: Long?,
        @RequestParam organizationId: Long
    ): ResponseEntity<InstructorOperatorProfileResponse> {
        val requiredActorUserId = authzGuardPort.requireActorUserId(actorUserId)
        val profile = instructorProfileService.getMyProfile(requiredActorUserId, organizationId)
        return ResponseEntity.ok(profile.toOperatorResponse(requireUser(profile.userId)))
    }

    @PostMapping("/me/instructor-profile")
    fun createMyProfile(
        @RequestHeader("X-Actor-User-Id", required = false) actorUserId: Long?,
        @RequestBody request: UpsertInstructorProfileWebRequest
    ): ResponseEntity<InstructorOperatorProfileResponse> {
        val requiredActorUserId = authzGuardPort.requireActorUserId(actorUserId)
        val profile = instructorProfileService.createMyProfile(
            UpsertInstructorProfileCommand(
                actorUserId = requiredActorUserId,
                organizationId = request.organizationId,
                headline = request.headline,
                bio = request.bio,
                languages = request.languages,
                specialties = request.specialties,
                certifications = request.certifications,
                yearsOfExperience = request.yearsOfExperience,
                internalNote = request.internalNote
            )
        )
        return ResponseEntity.status(201).body(profile.toOperatorResponse(requireUser(profile.userId)))
    }

    @PatchMapping("/me/instructor-profile")
    fun updateMyProfile(
        @RequestHeader("X-Actor-User-Id", required = false) actorUserId: Long?,
        @RequestBody request: UpsertInstructorProfileWebRequest
    ): ResponseEntity<InstructorOperatorProfileResponse> {
        val requiredActorUserId = authzGuardPort.requireActorUserId(actorUserId)
        val profile = instructorProfileService.updateMyProfile(
            UpsertInstructorProfileCommand(
                actorUserId = requiredActorUserId,
                organizationId = request.organizationId,
                headline = request.headline,
                bio = request.bio,
                languages = request.languages,
                specialties = request.specialties,
                certifications = request.certifications,
                yearsOfExperience = request.yearsOfExperience,
                internalNote = request.internalNote
            )
        )
        return ResponseEntity.ok(profile.toOperatorResponse(requireUser(profile.userId)))
    }

    @GetMapping("/instructors/{instructorProfileId}")
    fun getPublicProfile(@PathVariable instructorProfileId: Long): ResponseEntity<InstructorPublicProfileResponse> {
        val profile = instructorProfileService.getPublicProfile(instructorProfileId)
        return ResponseEntity.ok(profile.toPublicResponse(requireUser(profile.userId)))
    }

    private fun requireUser(userId: Long) = requireNotNull(userRepository.findById(userId)) {
        "Expected user $userId to exist"
    }
}
