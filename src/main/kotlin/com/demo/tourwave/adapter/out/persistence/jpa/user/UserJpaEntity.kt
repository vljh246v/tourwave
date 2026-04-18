package com.demo.tourwave.adapter.out.persistence.jpa.user

import com.demo.tourwave.domain.user.UserStatus
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
    name = "users",
    indexes = [Index(name = "uk_users_email", columnList = "email", unique = true)]
)
data class UserJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @Column(nullable = false)
    val name: String,
    @Column(nullable = false)
    val email: String,
    @Column(name = "password_hash", nullable = false)
    val passwordHash: String,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val status: UserStatus,
    @Column(name = "created_at", nullable = false)
    val createdAt: Instant,
    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant,
    @Column(name = "email_verified_at")
    val emailVerifiedAt: Instant? = null,
    @Column(name = "deleted_at_utc")
    val deletedAtUtc: Instant? = null
)
