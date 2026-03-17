package com.demo.tourwave.adapter.out.persistence.jpa.auth

import org.springframework.data.jpa.repository.JpaRepository

interface AuthRefreshTokenJpaRepository : JpaRepository<AuthRefreshTokenJpaEntity, Long> {
    fun findByTokenHash(tokenHash: String): AuthRefreshTokenJpaEntity?
    fun findByUserId(userId: Long): List<AuthRefreshTokenJpaEntity>
}
