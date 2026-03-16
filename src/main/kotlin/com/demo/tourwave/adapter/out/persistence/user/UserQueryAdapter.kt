package com.demo.tourwave.adapter.out.persistence.user

import com.demo.tourwave.application.user.port.UserRepository
import com.demo.tourwave.domain.user.User
import org.springframework.stereotype.Repository
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

@Repository
class UserQueryAdapter : UserRepository {
    private val sequence = AtomicLong(0L)
    private val usersById = ConcurrentHashMap<Long, User>()
    private val userIdByEmail = ConcurrentHashMap<String, Long>()

    override fun save(user: User): User {
        val normalizedEmail = user.email.trim().lowercase()
        val existingId = user.id ?: userIdByEmail[normalizedEmail]
        val userId = existingId ?: sequence.incrementAndGet()
        val persisted = user.copy(id = userId, email = normalizedEmail)
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

    override fun clear() {
        sequence.set(0L)
        usersById.clear()
        userIdByEmail.clear()
    }
}
