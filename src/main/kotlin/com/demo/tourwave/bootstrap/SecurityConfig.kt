package com.demo.tourwave.bootstrap

import com.demo.tourwave.adapter.`in`.web.error.ErrorResponse
import com.demo.tourwave.domain.common.DomainException
import com.demo.tourwave.domain.common.ErrorCode
import com.demo.tourwave.domain.user.UserStatus
import com.demo.tourwave.application.user.port.UserRepository
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.util.AntPathMatcher
import org.springframework.web.filter.OncePerRequestFilter

@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
class SecurityConfig(
    private val objectMapper: ObjectMapper,
    private val jwtTokenService: com.demo.tourwave.application.auth.JwtTokenService,
    private val userRepository: UserRepository,
    @Value("\${tourwave.auth.allow-header-auth-fallback:false}")
    private val allowHeaderAuthFallback: Boolean,
) {
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain =
        http
            .csrf { it.disable() }
            .httpBasic { it.disable() }
            .formLogin { it.disable() }
            .logout { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .exceptionHandling {
                it.authenticationEntryPoint(authenticationEntryPoint())
                it.accessDeniedHandler(accessDeniedHandler())
            }.addFilterBefore(authenticationFilter(), UsernamePasswordAuthenticationFilter::class.java)
            .authorizeHttpRequests {
                PUBLIC_ENDPOINTS.forEach { endpoint ->
                    it.requestMatchers(endpoint.method, endpoint.pattern).permitAll()
                }
                it.requestMatchers("/error").permitAll()
                it.anyRequest().authenticated()
            }.cors(Customizer.withDefaults())
            .build()

    @Bean
    fun authenticationFilter(): OncePerRequestFilter =
        object : OncePerRequestFilter() {
            private val pathMatcher = AntPathMatcher()

            override fun shouldNotFilter(request: HttpServletRequest): Boolean =
                PUBLIC_ENDPOINTS.any {
                    it.method.matches(request.method) && pathMatcher.match(it.pattern, request.servletPath)
                } || request.servletPath == "/error"

            override fun doFilterInternal(
                request: HttpServletRequest,
                response: HttpServletResponse,
                filterChain: FilterChain,
            ) {
                if (SecurityContextHolder.getContext().authentication == null) {
                    try {
                        resolveAuthentication(request)?.let { authentication ->
                            SecurityContextHolder.getContext().authentication = authentication
                        }
                    } catch (_: DomainException) {
                        SecurityContextHolder.clearContext()
                        writeError(response, HttpServletResponse.SC_UNAUTHORIZED, ErrorCode.UNAUTHORIZED, "Authentication is required")
                        return
                    }
                }
                filterChain.doFilter(request, response)
            }
        }

    @Bean
    fun authenticationEntryPoint(): AuthenticationEntryPoint =
        AuthenticationEntryPoint { _, response, _ ->
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, ErrorCode.UNAUTHORIZED, "Authentication is required")
        }

    @Bean
    fun accessDeniedHandler(): AccessDeniedHandler =
        AccessDeniedHandler { _, response, _ ->
            writeError(response, HttpServletResponse.SC_FORBIDDEN, ErrorCode.FORBIDDEN, "Access is denied")
        }

    private fun resolveAuthentication(request: HttpServletRequest): UsernamePasswordAuthenticationToken? {
        val authorization = request.getHeader("Authorization")
        if (authorization?.startsWith("Bearer ") == true) {
            val claims = jwtTokenService.parse(authorization.removePrefix("Bearer ").trim())
            requireActiveUser(claims.userId)
            return UsernamePasswordAuthenticationToken(
                claims.userId.toString(),
                null,
                claims.roles.map { SimpleGrantedAuthority("ROLE_${it.name}") },
            )
        }
        if (!allowHeaderAuthFallback) {
            return null
        }
        val actorUserId = request.getHeader("X-Actor-User-Id")?.trim()?.toLongOrNull() ?: return null
        val actorRole =
            request
                .getHeader("X-Actor-Role")
                ?.trim()
                ?.uppercase()
                ?.ifBlank { "USER" } ?: "USER"
        return UsernamePasswordAuthenticationToken(
            actorUserId.toString(),
            null,
            listOf(SimpleGrantedAuthority("ROLE_$actorRole")),
        )
    }

    private fun requireActiveUser(userId: Long) {
        val user =
            userRepository.findById(userId) ?: throw DomainException(
                errorCode = ErrorCode.UNAUTHORIZED,
                status = HttpServletResponse.SC_UNAUTHORIZED,
                message = "authenticated user does not exist",
            )
        if (user.status != UserStatus.ACTIVE) {
            throw DomainException(
                errorCode = ErrorCode.UNAUTHORIZED,
                status = HttpServletResponse.SC_UNAUTHORIZED,
                message = "account is not active",
            )
        }
    }

    private fun writeError(
        response: HttpServletResponse,
        status: Int,
        code: ErrorCode,
        message: String,
    ) {
        response.status = status
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.writer.write(
            objectMapper.writeValueAsString(
                ErrorResponse(
                    error =
                        ErrorResponse.ErrorDetail(
                            code = code,
                            message = message,
                        ),
                ),
            ),
        )
    }

    private data class PublicEndpoint(
        val method: HttpMethod,
        val pattern: String,
    )

    companion object {
        private val PUBLIC_ENDPOINTS =
            listOf(
                PublicEndpoint(HttpMethod.POST, "/auth/signup"),
                PublicEndpoint(HttpMethod.POST, "/auth/login"),
                PublicEndpoint(HttpMethod.POST, "/auth/refresh"),
                PublicEndpoint(HttpMethod.POST, "/auth/password/reset-request"),
                PublicEndpoint(HttpMethod.POST, "/auth/password/reset-confirm"),
                PublicEndpoint(HttpMethod.POST, "/auth/email/verify-confirm"),
                PublicEndpoint(HttpMethod.POST, "/payments/webhooks/provider"),
                PublicEndpoint(HttpMethod.GET, "/tours"),
                PublicEndpoint(HttpMethod.GET, "/tours/*"),
                PublicEndpoint(HttpMethod.GET, "/tours/*/occurrences"),
                PublicEndpoint(HttpMethod.GET, "/tours/*/content"),
                PublicEndpoint(HttpMethod.GET, "/occurrences/*"),
                PublicEndpoint(HttpMethod.GET, "/occurrences/*/availability"),
                PublicEndpoint(HttpMethod.GET, "/occurrences/*/quote"),
                PublicEndpoint(HttpMethod.GET, "/occurrences/*/calendar.ics"),
                PublicEndpoint(HttpMethod.GET, "/occurrences/*/reviews/summary"),
                PublicEndpoint(HttpMethod.GET, "/search/occurrences"),
                PublicEndpoint(HttpMethod.GET, "/organizations/*"),
                PublicEndpoint(HttpMethod.GET, "/instructors/*"),
                PublicEndpoint(HttpMethod.GET, "/actuator/health"),
                PublicEndpoint(HttpMethod.GET, "/actuator/health/**"),
                PublicEndpoint(HttpMethod.GET, "/actuator/metrics"),
                PublicEndpoint(HttpMethod.GET, "/actuator/metrics/**"),
                PublicEndpoint(HttpMethod.GET, "/actuator/info"),
            )
    }
}
