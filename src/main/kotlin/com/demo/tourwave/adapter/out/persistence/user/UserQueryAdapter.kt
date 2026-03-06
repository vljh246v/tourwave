package com.demo.tourwave.adapter.out.persistence.user

import com.demo.tourwave.application.user.port.UserQueryPort
import com.demo.tourwave.domain.user.User
import org.springframework.stereotype.Component

@Component
class UserQueryAdapter: UserQueryPort {
    override fun findByEmail(email: String): User? {
        TODO("Not yet implemented")
    }
}
