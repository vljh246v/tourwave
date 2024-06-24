package com.demo.tourwave.repository

import org.springframework.stereotype.Component
import com.demo.tourwave.domain.User
import com.demo.tourwave.domain.port.UserQueryPort

@Component
class UserPersistent: UserQueryPort {
    override fun getUserByEmail(email: String): User? {
        TODO("Not yet implemented")
    }
}
