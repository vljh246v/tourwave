package com.demo.tourwave.application.topology

data class ApplyInstructorRegistrationCommand(
    val actorUserId: Long,
    val organizationId: Long,
    val headline: String? = null,
    val bio: String? = null,
    val languages: List<String> = emptyList(),
    val specialties: List<String> = emptyList()
)

data class ReviewInstructorRegistrationCommand(
    val actorUserId: Long,
    val registrationId: Long,
    val rejectionReason: String? = null
)

data class UpsertInstructorProfileCommand(
    val actorUserId: Long,
    val organizationId: Long,
    val headline: String? = null,
    val bio: String? = null,
    val languages: List<String> = emptyList(),
    val specialties: List<String> = emptyList(),
    val certifications: List<String> = emptyList(),
    val yearsOfExperience: Int? = null,
    val internalNote: String? = null
)
