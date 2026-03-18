package com.demo.tourwave.adapter.out.persistence.jpa.customer

import org.springframework.data.jpa.repository.JpaRepository

interface NotificationJpaRepository : JpaRepository<NotificationJpaEntity, Long> {
    fun findByUserIdOrderByCreatedAtDesc(userId: Long): List<NotificationJpaEntity>
}
