package com.demo.tourwave.adapter.out.persistence.jpa.topology

import org.springframework.data.jpa.repository.JpaRepository

interface OrganizationMembershipJpaRepository : JpaRepository<OrganizationMembershipJpaEntity, Long> {
    fun findByOrganizationIdAndUserId(organizationId: Long, userId: Long): OrganizationMembershipJpaEntity?
    fun findByOrganizationIdOrderByIdAsc(organizationId: Long): List<OrganizationMembershipJpaEntity>
    fun findByUserIdOrderByIdAsc(userId: Long): List<OrganizationMembershipJpaEntity>
}
