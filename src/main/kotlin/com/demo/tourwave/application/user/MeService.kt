package com.demo.tourwave.application.user

import com.demo.tourwave.application.user.port.UserRepository
import com.demo.tourwave.domain.common.DomainException
import com.demo.tourwave.domain.common.ErrorCode
import com.demo.tourwave.domain.user.User
import java.time.Clock

class MeService(
    private val userRepository: UserRepository,
    private val clock: Clock
) {
    fun getCurrentUser(userId: Long): User {
        return userRepository.findById(userId) ?: throw DomainException(
            errorCode = ErrorCode.UNAUTHORIZED,
            status = 401,
            message = "authenticated user does not exist"
        )
    }

    fun updateCurrentUser(userId: Long, displayName: String): User {
        val normalizedDisplayName = com.demo.tourwave.application.auth.requireValidDisplayName(displayName)
        val user = getCurrentUser(userId)
        val updated = user.updateProfile(displayName = normalizedDisplayName, now = clock.instant())
        return userRepository.save(updated)
    }
}
