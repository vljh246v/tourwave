package com.demo.tourwave.adapter.`in`.web.auth

import com.demo.tourwave.application.auth.port.AuthRefreshTokenRepository
import com.demo.tourwave.application.auth.port.UserActionTokenRepository
import com.demo.tourwave.application.user.port.UserRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest(
    properties = [
        "tourwave.auth.allow-header-auth-fallback=false",
    ],
)
@AutoConfigureMockMvc
class RealModeSecurityIntegrationTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var authRefreshTokenRepository: AuthRefreshTokenRepository

    @Autowired
    private lateinit var userActionTokenRepository: UserActionTokenRepository

    @BeforeEach
    fun setUp() {
        authRefreshTokenRepository.clear()
        userActionTokenRepository.clear()
        userRepository.clear()
    }

    @Test
    fun `header fallback can be disabled for runtime security`() {
        mockMvc.perform(
            get("/me")
                .header("X-Actor-User-Id", "101"),
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"))
    }
}
