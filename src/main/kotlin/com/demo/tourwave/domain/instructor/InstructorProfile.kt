package com.demo.tourwave.domain.instructor

import java.time.Instant

data class InstructorProfile(
    val id: Long? = null,
    val userId: Long,
    val organizationId: Long,
    val headline: String? = null,
    val bio: String? = null,
    val languages: List<String> = emptyList(),
    val specialties: List<String> = emptyList(),
    val certifications: List<String> = emptyList(),
    val yearsOfExperience: Int? = null,
    val internalNote: String? = null,
    val status: InstructorProfileStatus = InstructorProfileStatus.ACTIVE,
    val approvedAt: Instant? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    fun update(
        headline: String?,
        bio: String?,
        languages: List<String>,
        specialties: List<String>,
        certifications: List<String>,
        yearsOfExperience: Int?,
        internalNote: String?,
        now: Instant,
    ): InstructorProfile {
        return copy(
            headline = headline,
            bio = bio,
            languages = languages,
            specialties = specialties,
            certifications = certifications,
            yearsOfExperience = yearsOfExperience,
            internalNote = internalNote,
            updatedAt = now,
        )
    }

    companion object {
        fun create(
            userId: Long,
            organizationId: Long,
            headline: String?,
            bio: String?,
            languages: List<String>,
            specialties: List<String>,
            certifications: List<String>,
            yearsOfExperience: Int?,
            internalNote: String?,
            approvedAt: Instant?,
            now: Instant,
        ): InstructorProfile {
            return InstructorProfile(
                userId = userId,
                organizationId = organizationId,
                headline = headline,
                bio = bio,
                languages = languages,
                specialties = specialties,
                certifications = certifications,
                yearsOfExperience = yearsOfExperience,
                internalNote = internalNote,
                status = InstructorProfileStatus.ACTIVE,
                approvedAt = approvedAt,
                createdAt = now,
                updatedAt = now,
            )
        }
    }
}
