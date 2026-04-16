package com.demo.tourwave.adapter.out.persistence.jpa.auth

import com.demo.tourwave.domain.auth.UserActionTokenPurpose
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(
    name = "user_action_tokens",
    indexes = [
        Index(name = "idx_user_action_tokens_user_purpose", columnList = "user_id,purpose"),
        Index(name = "uk_user_action_tokens_hash", columnList = "token_hash", unique = true),
    ],
)
data class UserActionTokenJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @Column(name = "user_id", nullable = false)
    val userId: Long,
    @Column(name = "token_hash", nullable = false)
    val tokenHash: String,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val purpose: UserActionTokenPurpose,
    @Column(name = "expires_at_utc", nullable = false)
    val expiresAtUtc: Instant,
    @Column(name = "created_at_utc", nullable = false)
    val createdAtUtc: Instant,
    @Column(name = "consumed_at_utc")
    val consumedAtUtc: Instant? = null,
)
