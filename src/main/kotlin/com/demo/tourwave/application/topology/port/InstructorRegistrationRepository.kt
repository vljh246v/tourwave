package com.demo.tourwave.application.topology.port

import com.demo.tourwave.domain.instructor.InstructorRegistration

interface InstructorRegistrationRepository {
    fun save(registration: InstructorRegistration): InstructorRegistration
    fun findById(registrationId: Long): InstructorRegistration?
    fun findByOrganizationId(organizationId: Long): List<InstructorRegistration>
    fun findByOrganizationIdAndUserId(organizationId: Long, userId: Long): InstructorRegistration?
    fun clear()
}
