package com.demo.tourwave.adapter.out.persistence.jpa.instructor

import org.springframework.data.jpa.repository.JpaRepository

interface InstructorRegistrationJpaRepository : JpaRepository<InstructorRegistrationJpaEntity, Long> {
    fun findByOrganizationIdOrderByIdAsc(organizationId: Long): List<InstructorRegistrationJpaEntity>
    fun findByOrganizationIdAndUserId(organizationId: Long, userId: Long): InstructorRegistrationJpaEntity?
}
