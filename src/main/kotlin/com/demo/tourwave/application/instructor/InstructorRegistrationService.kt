package com.demo.tourwave.application.instructor

import com.demo.tourwave.application.common.port.AuditEventCommand
import com.demo.tourwave.application.common.port.AuditEventPort
import com.demo.tourwave.application.instructor.port.InstructorProfileRepository
import com.demo.tourwave.application.instructor.port.InstructorRegistrationRepository
import com.demo.tourwave.application.organization.OrganizationAccessGuard
import com.demo.tourwave.application.organization.normalizeOptionalText
import com.demo.tourwave.application.organization.port.OrganizationRepository
import com.demo.tourwave.application.user.port.UserRepository
import com.demo.tourwave.domain.common.DomainException
import com.demo.tourwave.domain.common.ErrorCode
import com.demo.tourwave.domain.instructor.InstructorProfile
import com.demo.tourwave.domain.instructor.InstructorRegistration
import com.demo.tourwave.domain.instructor.InstructorRegistrationStatus
import com.demo.tourwave.domain.organization.OrganizationStatus
import org.springframework.transaction.annotation.Transactional
import java.time.Clock

@Transactional
class InstructorRegistrationService(
    private val registrationRepository: InstructorRegistrationRepository,
    private val instructorProfileRepository: InstructorProfileRepository,
    private val organizationRepository: OrganizationRepository,
    private val organizationAccessGuard: OrganizationAccessGuard,
    private val userRepository: UserRepository,
    private val auditEventPort: AuditEventPort,
    private val clock: Clock,
) {
    fun apply(command: ApplyInstructorRegistrationCommand): InstructorRegistration {
        userRepository.findById(command.actorUserId) ?: throw userNotFound(command.actorUserId)
        val organization = organizationRepository.findById(command.organizationId) ?: throw organizationNotFound(command.organizationId)
        if (organization.status != OrganizationStatus.ACTIVE) {
            throw organizationNotFound(command.organizationId)
        }

        val now = clock.instant()
        val existing = registrationRepository.findByOrganizationIdAndUserId(command.organizationId, command.actorUserId)
        val normalizedHeadline = normalizeOptionalHeadline(command.headline)
        val normalizedBio = normalizeOptionalInstructorBio(command.bio)
        val normalizedLanguages = normalizeStringList(command.languages, "languages")
        val normalizedSpecialties = normalizeStringList(command.specialties, "specialties")

        val saved =
            registrationRepository.save(
                when {
                    existing == null ->
                        InstructorRegistration.create(
                            organizationId = command.organizationId,
                            userId = command.actorUserId,
                            headline = normalizedHeadline,
                            bio = normalizedBio,
                            languages = normalizedLanguages,
                            specialties = normalizedSpecialties,
                            now = now,
                        )
                    existing.status == InstructorRegistrationStatus.REJECTED ->
                        existing.resubmit(
                            headline = normalizedHeadline,
                            bio = normalizedBio,
                            languages = normalizedLanguages,
                            specialties = normalizedSpecialties,
                            now = now,
                        )
                    else -> throw DomainException(
                        errorCode = ErrorCode.VALIDATION_ERROR,
                        status = 409,
                        message = "instructor registration already exists",
                    )
                },
            )
        auditEventPort.append(
            AuditEventCommand(
                actor = "USER:${command.actorUserId}",
                action = "INSTRUCTOR_REGISTRATION_SUBMITTED",
                resourceType = "INSTRUCTOR_REGISTRATION",
                resourceId = requireNotNull(saved.id),
                occurredAtUtc = clock.instant(),
                reasonCode = "INSTRUCTOR_REGISTRATION_SUBMITTED",
                afterJson = registrationSnapshot(saved),
            ),
        )
        return saved
    }

    fun approve(command: ReviewInstructorRegistrationCommand): InstructorRegistration {
        val registration = registrationRepository.findById(command.registrationId) ?: throw registrationNotFound(command.registrationId)
        organizationAccessGuard.requireOperator(command.actorUserId, registration.organizationId)
        if (registration.status != InstructorRegistrationStatus.PENDING) {
            throw invalidState("only pending registration can be approved")
        }

        val now = clock.instant()
        val approved = registrationRepository.save(registration.approve(command.actorUserId, now))
        val existingProfile =
            instructorProfileRepository.findByOrganizationIdAndUserId(
                registration.organizationId,
                registration.userId,
            )
        val wasNewProfile = existingProfile == null
        val mergedProfile =
            (
                existingProfile ?: InstructorProfile.create(
                    userId = registration.userId,
                    organizationId = registration.organizationId,
                    headline = approved.headline,
                    bio = approved.bio,
                    languages = approved.languages,
                    specialties = approved.specialties,
                    certifications = emptyList(),
                    yearsOfExperience = null,
                    internalNote = null,
                    approvedAt = now,
                    now = now,
                )
            ).update(
                headline = approved.headline,
                bio = approved.bio,
                languages = approved.languages,
                specialties = approved.specialties,
                certifications = existingProfile?.certifications ?: emptyList(),
                yearsOfExperience = existingProfile?.yearsOfExperience,
                internalNote = existingProfile?.internalNote,
                now = now,
            ).copy(approvedAt = existingProfile?.approvedAt ?: now)
        val savedProfile = instructorProfileRepository.save(mergedProfile)
        if (wasNewProfile) {
            auditEventPort.append(
                AuditEventCommand(
                    actor = "OPERATOR:${command.actorUserId}",
                    action = "INSTRUCTOR_PROFILE_CREATED",
                    resourceType = "INSTRUCTOR_PROFILE",
                    resourceId = requireNotNull(savedProfile.id),
                    occurredAtUtc = clock.instant(),
                    reasonCode = "INSTRUCTOR_PROFILE_CREATED",
                    afterJson = profileSnapshot(savedProfile),
                ),
            )
        }
        auditEventPort.append(
            AuditEventCommand(
                actor = "OPERATOR:${command.actorUserId}",
                action = "INSTRUCTOR_REGISTRATION_APPROVED",
                resourceType = "INSTRUCTOR_REGISTRATION",
                resourceId = requireNotNull(approved.id),
                occurredAtUtc = clock.instant(),
                reasonCode = "INSTRUCTOR_REGISTRATION_APPROVED",
                beforeJson = registrationSnapshot(registration),
                afterJson = registrationSnapshot(approved),
            ),
        )
        return approved
    }

    fun reject(command: ReviewInstructorRegistrationCommand): InstructorRegistration {
        val registration = registrationRepository.findById(command.registrationId) ?: throw registrationNotFound(command.registrationId)
        organizationAccessGuard.requireOperator(command.actorUserId, registration.organizationId)
        if (registration.status != InstructorRegistrationStatus.PENDING) {
            throw invalidState("only pending registration can be rejected")
        }
        val saved =
            registrationRepository.save(
                registration.reject(
                    reviewedByUserId = command.actorUserId,
                    rejectionReason = normalizeOptionalText(command.rejectionReason, 2000, "rejectionReason"),
                    now = clock.instant(),
                ),
            )
        auditEventPort.append(
            AuditEventCommand(
                actor = "OPERATOR:${command.actorUserId}",
                action = "INSTRUCTOR_REGISTRATION_REJECTED",
                resourceType = "INSTRUCTOR_REGISTRATION",
                resourceId = requireNotNull(saved.id),
                occurredAtUtc = clock.instant(),
                reasonCode = "INSTRUCTOR_REGISTRATION_REJECTED",
                beforeJson = registrationSnapshot(registration),
                afterJson = registrationSnapshot(saved),
            ),
        )
        return saved
    }

    private fun registrationSnapshot(registration: InstructorRegistration): Map<String, Any?> =
        mapOf(
            "organizationId" to registration.organizationId,
            "userId" to registration.userId,
            "status" to registration.status.name,
        )

    private fun profileSnapshot(profile: InstructorProfile): Map<String, Any?> =
        mapOf(
            "organizationId" to profile.organizationId,
            "userId" to profile.userId,
            "status" to profile.status.name,
        )

    fun listByOrganization(
        actorUserId: Long,
        organizationId: Long,
    ): List<InstructorRegistration> {
        organizationAccessGuard.requireOperator(actorUserId, organizationId)
        return registrationRepository.findByOrganizationId(organizationId)
    }

    private fun invalidState(message: String) =
        DomainException(
            errorCode = ErrorCode.INVALID_STATE_TRANSITION,
            status = 409,
            message = message,
        )

    private fun registrationNotFound(registrationId: Long) =
        DomainException(
            errorCode = ErrorCode.VALIDATION_ERROR,
            status = 404,
            message = "instructor registration $registrationId not found",
        )

    private fun organizationNotFound(organizationId: Long) =
        DomainException(
            errorCode = ErrorCode.VALIDATION_ERROR,
            status = 404,
            message = "organization $organizationId not found",
        )

    private fun userNotFound(userId: Long) =
        DomainException(
            errorCode = ErrorCode.UNAUTHORIZED,
            status = 401,
            message = "user $userId not found",
        )
}
