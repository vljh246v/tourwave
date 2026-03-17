package com.demo.tourwave.adapter.out.persistence.jpa.user

import com.demo.tourwave.application.user.port.UserRepository
import com.demo.tourwave.domain.user.User
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Repository

@Repository
@Profile("mysql", "mysql-test")
class JpaUserRepositoryAdapter(
    private val userJpaRepository: UserJpaRepository
) : UserRepository {
    override fun save(user: User): User = userJpaRepository.save(user.toEntity()).toDomain()

    override fun findById(userId: Long): User? = userJpaRepository.findById(userId).orElse(null)?.toDomain()

    override fun findByEmail(email: String): User? = userJpaRepository.findByEmail(email.trim().lowercase())?.toDomain()

    override fun clear() {
        userJpaRepository.deleteAllInBatch()
    }
}

private fun User.toEntity(): UserJpaEntity =
    UserJpaEntity(
        id = id,
        name = name,
        email = email.trim().lowercase()
    )

private fun UserJpaEntity.toDomain(): User =
    User(
        id = id,
        name = name,
        email = email
    )
