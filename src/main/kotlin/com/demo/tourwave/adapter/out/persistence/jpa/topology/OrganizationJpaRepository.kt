package com.demo.tourwave.adapter.out.persistence.jpa.topology

import org.springframework.data.jpa.repository.JpaRepository

interface OrganizationJpaRepository : JpaRepository<OrganizationJpaEntity, Long> {
    fun findBySlug(slug: String): OrganizationJpaEntity?
}
