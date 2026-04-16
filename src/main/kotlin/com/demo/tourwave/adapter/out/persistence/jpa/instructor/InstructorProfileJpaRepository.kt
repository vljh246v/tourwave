package com.demo.tourwave.adapter.out.persistence.jpa.instructor

import org.springframework.data.jpa.repository.JpaRepository

interface InstructorProfileJpaRepository : JpaRepository<InstructorProfileJpaEntity, Long> {
    fun findByOrganizationIdOrderByIdAsc(organizationId: Long): List<InstructorProfileJpaEntity>
    fun findByOrganizationIdAndUserId(organizationId: Long, userId: Long): InstructorProfileJpaEntity?
}
