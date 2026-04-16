package com.demo.tourwave.adapter.out.persistence.jpa.organization

import org.springframework.data.jpa.repository.JpaRepository

interface OrganizationJpaRepository : JpaRepository<OrganizationJpaEntity, Long> {
    fun findBySlug(slug: String): OrganizationJpaEntity?
}
