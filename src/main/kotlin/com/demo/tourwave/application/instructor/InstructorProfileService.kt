package com.demo.tourwave.application.instructor

import com.demo.tourwave.application.common.port.AuditEventCommand
import com.demo.tourwave.application.common.port.AuditEventPort
import com.demo.tourwave.application.instructor.port.InstructorProfileRepository
import com.demo.tourwave.application.instructor.port.InstructorRegistrationRepository
import com.demo.tourwave.application.user.port.UserRepository
import com.demo.tourwave.domain.common.DomainException
import com.demo.tourwave.domain.common.ErrorCode
import com.demo.tourwave.domain.instructor.InstructorProfile
import com.demo.tourwave.domain.instructor.InstructorProfileStatus
import com.demo.tourwave.domain.instructor.InstructorRegistrationStatus
import org.springframework.transaction.annotation.Transactional
import java.time.Clock

@Transactional
class InstructorProfileService(
    private val instructorProfileRepository: InstructorProfileRepository,
    private val instructorRegistrationRepository: InstructorRegistrationRepository,
    private val userRepository: UserRepository,
    private val auditEventPort: AuditEventPort,
    private val clock: Clock,
) {
    fun getMyProfile(
        actorUserId: Long,
        organizationId: Long,
    ): InstructorProfile {
        userRepository.findById(actorUserId) ?: throw unauthorized()
        return instructorProfileRepository.findByOrganizationIdAndUserId(organizationId, actorUserId)
            ?: throw notFound(organizationId, actorUserId)
    }

    fun createMyProfile(command: UpsertInstructorProfileCommand): InstructorProfile {
        userRepository.findById(command.actorUserId) ?: throw unauthorized()
        if (instructorProfileRepository.findByOrganizationIdAndUserId(command.organizationId, command.actorUserId) != null) {
            throw DomainException(
                errorCode = ErrorCode.VALIDATION_ERROR,
                status = 409,
                message = "instructor profile already exists",
            )
        }
        val registration =
            instructorRegistrationRepository
                .findByOrganizationIdAndUserId(command.organizationId, command.actorUserId)
                ?.takeIf { it.status == InstructorRegistrationStatus.APPROVED }
                ?: throw DomainException(
                    errorCode = ErrorCode.FORBIDDEN,
                    status = 403,
                    message = "approved instructor registration is required",
                )

        val saved =
            instructorProfileRepository.save(
                InstructorProfile.create(
                    userId = command.actorUserId,
                    organizationId = command.organizationId,
                    headline = normalizeOptionalHeadline(command.headline ?: registration.headline),
                    bio = normalizeOptionalInstructorBio(command.bio ?: registration.bio),
                    languages =
                        normalizeStringList(
                            command.languages.ifEmpty { registration.languages },
                            "languages",
                        ),
                    specialties =
                        normalizeStringList(
                            command.specialties.ifEmpty { registration.specialties },
                            "specialties",
                        ),
                    certifications = normalizeStringList(command.certifications, "certifications"),
                    yearsOfExperience = normalizeYearsOfExperience(command.yearsOfExperience),
                    internalNote = normalizeOptionalInternalNote(command.internalNote),
                    approvedAt = clock.instant(),
                    now = clock.instant(),
                ),
            )
        auditEventPort.append(
            AuditEventCommand(
                actor = "USER:${command.actorUserId}",
                action = "INSTRUCTOR_PROFILE_CREATED",
                resourceType = "INSTRUCTOR_PROFILE",
                resourceId = requireNotNull(saved.id),
                occurredAtUtc = clock.instant(),
                reasonCode = "INSTRUCTOR_PROFILE_CREATED",
                afterJson = profileSnapshot(saved),
            ),
        )
        return saved
    }

    fun updateMyProfile(command: UpsertInstructorProfileCommand): InstructorProfile {
        userRepository.findById(command.actorUserId) ?: throw unauthorized()
        val profile =
            instructorProfileRepository.findByOrganizationIdAndUserId(command.organizationId, command.actorUserId)
                ?: throw notFound(command.organizationId, command.actorUserId)
        val saved =
            instructorProfileRepository.save(
                profile.update(
                    headline = normalizeOptionalHeadline(command.headline),
                    bio = normalizeOptionalInstructorBio(command.bio),
                    languages = normalizeStringList(command.languages, "languages"),
                    specialties = normalizeStringList(command.specialties, "specialties"),
                    certifications = normalizeStringList(command.certifications, "certifications"),
                    yearsOfExperience = normalizeYearsOfExperience(command.yearsOfExperience),
                    internalNote = normalizeOptionalInternalNote(command.internalNote),
                    now = clock.instant(),
                ),
            )
        auditEventPort.append(
            AuditEventCommand(
                actor = "USER:${command.actorUserId}",
                action = "INSTRUCTOR_PROFILE_UPDATED",
                resourceType = "INSTRUCTOR_PROFILE",
                resourceId = requireNotNull(saved.id),
                occurredAtUtc = clock.instant(),
                reasonCode = "INSTRUCTOR_PROFILE_UPDATED",
                beforeJson = profileSnapshot(profile),
                afterJson = profileSnapshot(saved),
            ),
        )
        return saved
    }

    fun getPublicProfile(instructorProfileId: Long): InstructorProfile {
        val profile = instructorProfileRepository.findById(instructorProfileId) ?: throw publicNotFound(instructorProfileId)
        if (profile.status != InstructorProfileStatus.ACTIVE) {
            throw publicNotFound(instructorProfileId)
        }
        return profile
    }

    private fun profileSnapshot(profile: InstructorProfile): Map<String, Any?> =
        mapOf(
            "organizationId" to profile.organizationId,
            "userId" to profile.userId,
            "status" to profile.status.name,
        )

    private fun unauthorized() =
        DomainException(
            errorCode = ErrorCode.UNAUTHORIZED,
            status = 401,
            message = "authenticated user does not exist",
        )

    private fun notFound(
        organizationId: Long,
        actorUserId: Long,
    ) = DomainException(
        errorCode = ErrorCode.VALIDATION_ERROR,
        status = 404,
        message = "instructor profile for organization $organizationId and user $actorUserId not found",
    )

    private fun publicNotFound(instructorProfileId: Long) =
        DomainException(
            errorCode = ErrorCode.VALIDATION_ERROR,
            status = 404,
            message = "instructor profile $instructorProfileId not found",
        )
}
