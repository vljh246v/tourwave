package com.demo.tourwave.adapter.`in`.web.auth

import com.demo.tourwave.application.auth.AuthCommandService
import com.demo.tourwave.application.auth.AuthResult
import com.demo.tourwave.application.common.port.AuthzGuardPort
import com.demo.tourwave.domain.user.User
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseCookie
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController
import java.time.Duration

@RestController
class AuthController(
    private val authCommandService: AuthCommandService,
    private val authzGuardPort: AuthzGuardPort,
) {
    @PostMapping("/auth/signup")
    fun signup(
        @RequestBody request: SignupRequest,
    ): ResponseEntity<AuthResponse> {
        val result =
            authCommandService.signup(
                displayName = request.displayName,
                email = request.email,
                password = request.password,
            )
        return ResponseEntity.status(201).body(result.toWebResponse())
    }

    @PostMapping("/auth/login")
    fun login(
        @RequestBody request: LoginRequest,
    ): ResponseEntity<AuthResponse> {
        val result =
            authCommandService.login(
                email = request.email,
                password = request.password,
            )
        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, buildAccessCookie(result.accessToken).toString())
            .header(HttpHeaders.SET_COOKIE, buildRefreshCookie(result.refreshToken).toString())
            .body(result.toWebResponse())
    }

    @PostMapping("/auth/refresh")
    fun refresh(
        @RequestBody request: RefreshRequest,
    ): ResponseEntity<AuthResponse> {
        val result = authCommandService.refresh(request.refreshToken)
        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, buildAccessCookie(result.accessToken).toString())
            .header(HttpHeaders.SET_COOKIE, buildRefreshCookie(result.refreshToken).toString())
            .body(result.toWebResponse())
    }

    @PostMapping("/auth/logout")
    fun logout(
        @RequestHeader("X-Actor-User-Id", required = false) actorUserId: Long?,
    ): ResponseEntity<Void> {
        val requiredActorUserId = authzGuardPort.requireActorUserId(actorUserId)
        authCommandService.logout(requiredActorUserId)
        val clearAccess = ResponseCookie.from("access_token", "")
            .httpOnly(true)
            .secure(false)
            .sameSite("Strict")
            .path("/")
            .maxAge(Duration.ZERO)
            .build()
        val clearRefresh = ResponseCookie.from("refresh_token", "")
            .httpOnly(true)
            .secure(false)
            .sameSite("Strict")
            .path("/auth/refresh")
            .maxAge(Duration.ZERO)
            .build()
        return ResponseEntity.noContent()
            .header(HttpHeaders.SET_COOKIE, clearAccess.toString())
            .header(HttpHeaders.SET_COOKIE, clearRefresh.toString())
            .build()
    }

    @PostMapping("/auth/email/verify-request")
    fun requestEmailVerification(
        @RequestHeader("X-Actor-User-Id", required = false) actorUserId: Long?,
    ): ResponseEntity<Void> {
        authCommandService.requestEmailVerification(authzGuardPort.requireActorUserId(actorUserId))
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/auth/email/verify-confirm")
    fun confirmEmailVerification(
        @RequestBody request: EmailVerifyConfirmRequest,
    ): ResponseEntity<Void> {
        authCommandService.confirmEmailVerification(request.token)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/auth/password/reset-request")
    fun requestPasswordReset(
        @RequestBody request: PasswordResetRequest,
    ): ResponseEntity<Void> {
        authCommandService.requestPasswordReset(request.email)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/auth/password/reset-confirm")
    fun confirmPasswordReset(
        @RequestBody request: PasswordResetConfirmRequest,
    ): ResponseEntity<Void> {
        authCommandService.confirmPasswordReset(
            token = request.token,
            newPassword = request.password,
        )
        return ResponseEntity.noContent().build()
    }
}

data class SignupRequest(
    val email: String,
    val password: String,
    val displayName: String,
)

data class LoginRequest(
    val email: String,
    val password: String,
)

data class RefreshRequest(
    val refreshToken: String,
)

data class EmailVerifyConfirmRequest(
    val token: String,
)

data class PasswordResetRequest(
    val email: String,
)

data class PasswordResetConfirmRequest(
    val token: String,
    val password: String,
)

data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val user: AuthUserSummary,
)

data class AuthUserSummary(
    val id: Long,
    val email: String,
    val displayName: String,
)

private fun buildAccessCookie(token: String): ResponseCookie =
    ResponseCookie.from("access_token", token)
        .httpOnly(true)
        .secure(false)
        .sameSite("Strict")
        .path("/")
        .maxAge(Duration.ofSeconds(3600))
        .build()

private fun buildRefreshCookie(token: String): ResponseCookie =
    ResponseCookie.from("refresh_token", token)
        .httpOnly(true)
        .secure(false)
        .sameSite("Strict")
        .path("/auth/refresh")
        .maxAge(Duration.ofDays(14))
        .build()

private fun AuthResult.toWebResponse(): AuthResponse =
    AuthResponse(
        accessToken = accessToken,
        refreshToken = refreshToken,
        user = user.toAuthUserSummary(),
    )

private fun User.toAuthUserSummary(): AuthUserSummary =
    AuthUserSummary(
        id = requireNotNull(id),
        email = email,
        displayName = displayName,
    )
