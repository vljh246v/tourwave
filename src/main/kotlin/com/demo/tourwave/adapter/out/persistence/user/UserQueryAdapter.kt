package com.demo.tourwave.adapter.out.persistence.user

import com.demo.tourwave.application.user.UserPort
import com.demo.tourwave.application.user.port.UserRepository
import com.demo.tourwave.domain.user.User
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Repository
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

@Repository
@Profile("!mysql & !mysql-test")
class UserQueryAdapter : UserRepository, UserPort {
    private val sequence = AtomicLong(0L)
    private val usersById = ConcurrentHashMap<Long, User>()
    private val userIdByEmail = ConcurrentHashMap<String, Long>()

    override fun save(user: User): User {
        val normalizedEmail = user.email.trim().lowercase()
        val existingId = user.id ?: userIdByEmail[normalizedEmail]
        val userId = existingId ?: sequence.incrementAndGet()
        val existing = usersById[userId]
        val persisted =
            user.copy(
                id = userId,
                email = normalizedEmail,
                status = user.status,
                createdAt = existing?.createdAt ?: user.createdAt,
                updatedAt = if (existing == null) user.updatedAt else user.updatedAt,
                emailVerifiedAt = user.emailVerifiedAt ?: existing?.emailVerifiedAt,
            )
        usersById[userId] = persisted
        userIdByEmail[normalizedEmail] = userId
        return persisted
    }

    override fun findById(userId: Long): User? {
        return usersById[userId]
    }

    override fun findByEmail(email: String): User? {
        return userIdByEmail[email.trim().lowercase()]
            ?.let(usersById::get)
    }

    override fun findAll(): List<User> = usersById.values.sortedBy { it.id }

    override fun deleteById(userId: Long) {
        val user = usersById.remove(userId)
        if (user != null) {
            userIdByEmail.remove(user.email)
        }
    }

    override fun clear() {
        sequence.set(0L)
        usersById.clear()
        userIdByEmail.clear()
    }
}
