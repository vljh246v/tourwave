package com.demo.tourwave.adapter.out.persistence.jpa.auth

import com.demo.tourwave.domain.auth.UserActionTokenPurpose
import org.springframework.data.jpa.repository.JpaRepository

interface UserActionTokenJpaRepository : JpaRepository<UserActionTokenJpaEntity, Long> {
    fun findByTokenHash(tokenHash: String): UserActionTokenJpaEntity?
    fun findAllByUserIdAndPurposeAndConsumedAtUtcIsNullOrderByCreatedAtUtcDesc(
        userId: Long,
        purpose: UserActionTokenPurpose
    ): List<UserActionTokenJpaEntity>
}
