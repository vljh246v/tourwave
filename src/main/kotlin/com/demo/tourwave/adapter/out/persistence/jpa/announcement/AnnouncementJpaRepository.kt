package com.demo.tourwave.adapter.out.persistence.jpa.announcement

import org.springframework.data.jpa.repository.JpaRepository

interface AnnouncementJpaRepository : JpaRepository<AnnouncementJpaEntity, Long> {
    fun findByOrganizationIdOrderByUpdatedAtDescIdDesc(organizationId: Long): List<AnnouncementJpaEntity>
}
