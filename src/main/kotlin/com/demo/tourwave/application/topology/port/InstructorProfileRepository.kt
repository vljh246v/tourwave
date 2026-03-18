package com.demo.tourwave.application.topology.port

import com.demo.tourwave.domain.instructor.InstructorProfile

interface InstructorProfileRepository {
    fun save(instructorProfile: InstructorProfile): InstructorProfile
    fun findById(instructorProfileId: Long): InstructorProfile?
    fun findByOrganizationId(organizationId: Long): List<InstructorProfile>
    fun findByOrganizationIdAndUserId(organizationId: Long, userId: Long): InstructorProfile?
    fun clear()
}
