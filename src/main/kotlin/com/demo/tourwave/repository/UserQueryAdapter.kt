package com.demo.tourwave.repository

import org.springframework.stereotype.Component
import com.demo.tourwave.domain.user.User
import com.demo.tourwave.domain.user.port.UserQueryPort

@Component
class UserQueryAdapter: UserQueryPort {
    override fun findByEmail(email: String): User? {
        TODO("Not yet implemented")
    }
}
