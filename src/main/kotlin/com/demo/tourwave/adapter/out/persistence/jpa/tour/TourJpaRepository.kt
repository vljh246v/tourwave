package com.demo.tourwave.adapter.out.persistence.jpa.tour

import org.springframework.data.jpa.repository.JpaRepository

interface TourJpaRepository : JpaRepository<TourJpaEntity, Long> {
    fun findByOrganizationIdOrderByIdAsc(organizationId: Long): List<TourJpaEntity>

    fun findByStatusOrderByIdAsc(status: com.demo.tourwave.domain.tour.TourStatus): List<TourJpaEntity>
}
