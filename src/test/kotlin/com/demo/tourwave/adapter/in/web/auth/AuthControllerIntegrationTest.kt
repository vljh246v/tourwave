package com.demo.tourwave.adapter.`in`.web.auth

import com.demo.tourwave.application.auth.port.AuthRefreshTokenRepository
import com.demo.tourwave.application.auth.port.UserActionTokenRepository
import com.demo.tourwave.application.user.port.UserRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerIntegrationTest {
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
    fun `signup login refresh logout and me endpoints work with jwt`() {
        val signupResult = mockMvc.perform(
            post("/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"jae@test.com","password":"Password12","displayName":"Jae"}""")
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.user.email").value("jae@test.com"))
            .andReturn()

        val accessToken = JsonFieldReader.read(signupResult.response.contentAsString, "accessToken")
        val refreshToken = JsonFieldReader.read(signupResult.response.contentAsString, "refreshToken")

        mockMvc.perform(
            get("/me")
                .header("Authorization", "Bearer $accessToken")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.user.displayName").value("Jae"))
            .andExpect(jsonPath("$.memberships").isArray)

        mockMvc.perform(
            patch("/me")
                .header("Authorization", "Bearer $accessToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"displayName":"Jae Updated"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.displayName").value("Jae Updated"))

        val refreshResult = mockMvc.perform(
            post("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"refreshToken":"$refreshToken"}""")
        )
            .andExpect(status().isOk)
            .andReturn()

        val rotatedAccessToken = JsonFieldReader.read(refreshResult.response.contentAsString, "accessToken")

        mockMvc.perform(
            post("/auth/logout")
                .header("Authorization", "Bearer $rotatedAccessToken")
        )
            .andExpect(status().isNoContent)

        mockMvc.perform(
            post("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"refreshToken":"$refreshToken"}""")
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `me rejects missing authentication`() {
        mockMvc.perform(get("/me"))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"))
    }
}

private object JsonFieldReader {
    private val mapper = com.fasterxml.jackson.module.kotlin.jacksonObjectMapper()

    fun read(json: String, field: String): String {
        return requireNotNull(mapper.readTree(json).get(field)?.asText())
    }
}
