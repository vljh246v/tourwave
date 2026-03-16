package com.demo.tourwave.domain.instructor

data class InstructorProfile(
    val id: Long? = null,
    val userId: Long,
    val organizationId: Long,
    val bio: String? = null,
    val languages: List<String> = emptyList()
)
