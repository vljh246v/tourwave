package com.demo.tourwave.adapter.`in`.web.auth

import com.demo.tourwave.adapter.out.persistence.audit.InMemoryAuditEventAdapter
import com.demo.tourwave.application.auth.ActionTokenGenerator
import com.demo.tourwave.application.auth.port.AuthRefreshTokenRepository
import com.demo.tourwave.application.auth.port.UserActionTokenRepository
import com.demo.tourwave.application.user.port.UserRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.mockito.kotlin.whenever

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

    @Autowired
    private lateinit var auditEventAdapter: InMemoryAuditEventAdapter

    @MockBean
    private lateinit var actionTokenGenerator: ActionTokenGenerator

    @BeforeEach
    fun setUp() {
        authRefreshTokenRepository.clear()
        userActionTokenRepository.clear()
        userRepository.clear()
        auditEventAdapter.clear()
        whenever(actionTokenGenerator.generate())
            .thenReturn("signup-verify-token", "reset-token", "extra-verify-token")
    }

    @Test
    fun `signup verify reset deactivate and auth endpoints work with jwt`() {
        val signupResult = mockMvc.perform(
            post("/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"jae@test.com","password":"Password12","displayName":"Jae"}""")
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.user.email").value("jae@test.com"))
            .andReturn()

        val accessToken = JsonFieldReader.read(signupResult.response.contentAsString, "accessToken")
        mockMvc.perform(
            get("/me")
                .header("Authorization", "Bearer $accessToken")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.user.displayName").value("Jae"))
            .andExpect(jsonPath("$.memberships").isArray)

        mockMvc.perform(
            post("/auth/email/verify-confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"token":"signup-verify-token"}""")
        )
            .andExpect(status().isNoContent)

        mockMvc.perform(
            post("/auth/email/verify-confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"token":"signup-verify-token"}""")
        )
            .andExpect(status().isBadRequest)

        mockMvc.perform(
            post("/auth/password/reset-request")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"missing@test.com"}""")
        )
            .andExpect(status().isNoContent)

        mockMvc.perform(
            post("/auth/password/reset-request")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"jae@test.com"}""")
        )
            .andExpect(status().isNoContent)

        mockMvc.perform(
            post("/auth/password/reset-confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"token":"reset-token","password":"Password34"}""")
        )
            .andExpect(status().isNoContent)

        mockMvc.perform(
            post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"jae@test.com","password":"Password12"}""")
        )
            .andExpect(status().isUnauthorized)

        val loginResult = mockMvc.perform(
            post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"jae@test.com","password":"Password34"}""")
        )
            .andExpect(status().isOk)
            .andReturn()

        val resetAccessToken = JsonFieldReader.read(loginResult.response.contentAsString, "accessToken")
        val resetRefreshToken = JsonFieldReader.read(loginResult.response.contentAsString, "refreshToken")

        mockMvc.perform(
            post("/operator/organizations")
                .header("Authorization", "Bearer $resetAccessToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"slug":"jae-ops","name":"Jae Ops","timezone":"Asia/Seoul"}""")
        )
            .andExpect(status().isCreated)

        mockMvc.perform(
            patch("/me")
                .header("Authorization", "Bearer $resetAccessToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"displayName":"Jae Updated"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.displayName").value("Jae Updated"))

        val refreshResult = mockMvc.perform(
            post("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"refreshToken":"$resetRefreshToken"}""")
        )
            .andExpect(status().isOk)
            .andReturn()

        val rotatedAccessToken = JsonFieldReader.read(refreshResult.response.contentAsString, "accessToken")

        mockMvc.perform(
            post("/auth/email/verify-request")
                .header("Authorization", "Bearer $rotatedAccessToken")
        )
            .andExpect(status().isNoContent)

        mockMvc.perform(
            post("/me/deactivate")
                .header("Authorization", "Bearer $rotatedAccessToken")
        )
            .andExpect(status().isNoContent)

        mockMvc.perform(
            get("/me")
                .header("Authorization", "Bearer $rotatedAccessToken")
        ).andExpect(status().isUnauthorized)

        mockMvc.perform(
            post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"jae@test.com","password":"Password34"}""")
        )
            .andExpect(status().isUnauthorized)

        mockMvc.perform(
            post("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"refreshToken":"$resetRefreshToken"}""")
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `me rejects missing authentication`() {
        mockMvc.perform(get("/me"))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"))
    }

    @Test
    fun `public catalog remains accessible without authentication`() {
        mockMvc.perform(get("/tours"))
            .andExpect(status().isOk)
    }
}

private object JsonFieldReader {
    private val mapper = com.fasterxml.jackson.module.kotlin.jacksonObjectMapper()

    fun read(json: String, field: String): String {
        return requireNotNull(mapper.readTree(json).get(field)?.asText())
    }
}
