package com.demo.tourwave.domain.instructor

import java.time.Instant

data class InstructorRegistration(
    val id: Long? = null,
    val organizationId: Long,
    val userId: Long,
    val headline: String? = null,
    val bio: String? = null,
    val languages: List<String> = emptyList(),
    val specialties: List<String> = emptyList(),
    val status: InstructorRegistrationStatus,
    val rejectionReason: String? = null,
    val reviewedByUserId: Long? = null,
    val reviewedAt: Instant? = null,
    val createdAt: Instant,
    val updatedAt: Instant
) {
    fun resubmit(
        headline: String?,
        bio: String?,
        languages: List<String>,
        specialties: List<String>,
        now: Instant
    ): InstructorRegistration {
        return copy(
            headline = headline,
            bio = bio,
            languages = languages,
            specialties = specialties,
            status = InstructorRegistrationStatus.PENDING,
            rejectionReason = null,
            reviewedByUserId = null,
            reviewedAt = null,
            updatedAt = now
        )
    }

    fun approve(reviewedByUserId: Long, now: Instant): InstructorRegistration {
        return copy(
            status = InstructorRegistrationStatus.APPROVED,
            rejectionReason = null,
            reviewedByUserId = reviewedByUserId,
            reviewedAt = now,
            updatedAt = now
        )
    }

    fun reject(reviewedByUserId: Long, rejectionReason: String?, now: Instant): InstructorRegistration {
        return copy(
            status = InstructorRegistrationStatus.REJECTED,
            rejectionReason = rejectionReason,
            reviewedByUserId = reviewedByUserId,
            reviewedAt = now,
            updatedAt = now
        )
    }

    companion object {
        fun create(
            organizationId: Long,
            userId: Long,
            headline: String?,
            bio: String?,
            languages: List<String>,
            specialties: List<String>,
            now: Instant
        ): InstructorRegistration {
            return InstructorRegistration(
                organizationId = organizationId,
                userId = userId,
                headline = headline,
                bio = bio,
                languages = languages,
                specialties = specialties,
                status = InstructorRegistrationStatus.PENDING,
                createdAt = now,
                updatedAt = now
            )
        }
    }
}
