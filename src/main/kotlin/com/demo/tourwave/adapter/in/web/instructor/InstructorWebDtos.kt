package com.demo.tourwave.adapter.`in`.web.instructor

import com.demo.tourwave.domain.instructor.InstructorProfile
import com.demo.tourwave.domain.instructor.InstructorRegistration
import com.demo.tourwave.domain.user.User
import java.time.Instant

data class ApplyInstructorRegistrationWebRequest(
    val organizationId: Long,
    val headline: String? = null,
    val bio: String? = null,
    val languages: List<String> = emptyList(),
    val specialties: List<String> = emptyList(),
)

data class RejectInstructorRegistrationWebRequest(
    val rejectionReason: String? = null,
)

data class UpsertInstructorProfileWebRequest(
    val organizationId: Long,
    val headline: String? = null,
    val bio: String? = null,
    val languages: List<String> = emptyList(),
    val specialties: List<String> = emptyList(),
    val certifications: List<String> = emptyList(),
    val yearsOfExperience: Int? = null,
    val internalNote: String? = null,
)

data class InstructorRegistrationResponse(
    val id: Long,
    val organizationId: Long,
    val userId: Long,
    val userDisplayName: String,
    val headline: String?,
    val bio: String?,
    val languages: List<String>,
    val specialties: List<String>,
    val status: String,
    val rejectionReason: String?,
    val reviewedByUserId: Long?,
    val reviewedAt: Instant?,
)

data class InstructorOperatorProfileResponse(
    val id: Long,
    val organizationId: Long,
    val userId: Long,
    val displayName: String,
    val headline: String?,
    val bio: String?,
    val languages: List<String>,
    val specialties: List<String>,
    val certifications: List<String>,
    val yearsOfExperience: Int?,
    val internalNote: String?,
    val status: String,
    val approvedAt: Instant?,
)

data class InstructorPublicProfileResponse(
    val id: Long,
    val organizationId: Long,
    val displayName: String,
    val headline: String?,
    val bio: String?,
    val languages: List<String>,
    val specialties: List<String>,
    val yearsOfExperience: Int?,
)

fun InstructorRegistration.toResponse(user: User): InstructorRegistrationResponse =
    InstructorRegistrationResponse(
        id = requireNotNull(id),
        organizationId = organizationId,
        userId = userId,
        userDisplayName = user.displayName,
        headline = headline,
        bio = bio,
        languages = languages,
        specialties = specialties,
        status = status.name,
        rejectionReason = rejectionReason,
        reviewedByUserId = reviewedByUserId,
        reviewedAt = reviewedAt,
    )

fun InstructorProfile.toOperatorResponse(user: User): InstructorOperatorProfileResponse =
    InstructorOperatorProfileResponse(
        id = requireNotNull(id),
        organizationId = organizationId,
        userId = userId,
        displayName = user.displayName,
        headline = headline,
        bio = bio,
        languages = languages,
        specialties = specialties,
        certifications = certifications,
        yearsOfExperience = yearsOfExperience,
        internalNote = internalNote,
        status = status.name,
        approvedAt = approvedAt,
    )

fun InstructorProfile.toPublicResponse(user: User): InstructorPublicProfileResponse =
    InstructorPublicProfileResponse(
        id = requireNotNull(id),
        organizationId = organizationId,
        displayName = user.displayName,
        headline = headline,
        bio = bio,
        languages = languages,
        specialties = specialties,
        yearsOfExperience = yearsOfExperience,
    )
