package com.demo.tourwave.repository

import org.springframework.stereotype.Component
import com.demo.tourwave.domain.user.User
import com.demo.tourwave.domain.user.port.UserQueryPort

@Component
class UserPersistent: UserQueryPort {
    override fun getUserByEmail(email: String): User? {
        TODO("Not yet implemented")
    }
}
