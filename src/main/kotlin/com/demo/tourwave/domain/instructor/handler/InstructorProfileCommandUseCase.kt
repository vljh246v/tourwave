package com.demo.tourwave.domain.instructor.handler

import com.demo.tourwave.domain.instructor.InstructorProfile

interface InstructorProfileCommandUseCase {
    fun createProfile(
        userId: Long,
        organizationId: Long,
        bio: String? = null,
        languages: List<String> = emptyList()
    ): InstructorProfile
}

