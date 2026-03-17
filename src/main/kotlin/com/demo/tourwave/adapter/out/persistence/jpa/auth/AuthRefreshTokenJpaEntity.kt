package com.demo.tourwave.adapter.out.persistence.jpa.auth

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(
    name = "auth_refresh_tokens",
    indexes = [
        Index(name = "idx_auth_refresh_tokens_user", columnList = "user_id"),
        Index(name = "uk_auth_refresh_tokens_hash", columnList = "token_hash", unique = true)
    ]
)
data class AuthRefreshTokenJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @Column(name = "user_id", nullable = false)
    val userId: Long,
    @Column(name = "token_hash", nullable = false)
    val tokenHash: String,
    @Column(name = "expires_at_utc", nullable = false)
    val expiresAtUtc: Instant,
    @Column(name = "issued_at_utc", nullable = false)
    val issuedAtUtc: Instant,
    @Column(name = "revoked_at_utc")
    val revokedAtUtc: Instant? = null
)
