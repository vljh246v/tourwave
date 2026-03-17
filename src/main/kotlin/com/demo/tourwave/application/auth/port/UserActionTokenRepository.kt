package com.demo.tourwave.application.auth.port

import com.demo.tourwave.domain.auth.UserActionToken

interface UserActionTokenRepository {
    fun save(token: UserActionToken): UserActionToken
    fun clear()
}
