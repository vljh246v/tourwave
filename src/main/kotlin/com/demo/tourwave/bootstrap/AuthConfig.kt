package com.demo.tourwave.bootstrap

import com.demo.tourwave.application.auth.AuthCommandService
import com.demo.tourwave.application.auth.AuthTokenLifecycleService
import com.demo.tourwave.application.auth.JwtTokenService
import com.demo.tourwave.application.auth.port.AuthRefreshTokenRepository
import com.demo.tourwave.application.auth.port.PasswordHasher
import com.demo.tourwave.application.auth.port.UserActionTokenRepository
import com.demo.tourwave.application.topology.OrganizationQueryService
import com.demo.tourwave.application.user.MeService
import com.demo.tourwave.application.user.port.UserRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock

@Configuration
class AuthConfig {
    @Bean
    fun jwtTokenService(
        @Value("\${tourwave.auth.jwt-secret}") jwtSecret: String,
        @Value("\${tourwave.auth.access-token-ttl-seconds}") accessTokenTtlSeconds: Long,
        clock: Clock,
        objectMapper: ObjectMapper
    ): JwtTokenService {
        return JwtTokenService(
            secret = jwtSecret,
            accessTokenTtlSeconds = accessTokenTtlSeconds,
            clock = clock,
            objectMapper = objectMapper
        )
    }

    @Bean
    fun authTokenLifecycleService(
        authRefreshTokenRepository: AuthRefreshTokenRepository,
        @Value("\${tourwave.auth.refresh-token-ttl-seconds}") refreshTokenTtlSeconds: Long,
        clock: Clock
    ): AuthTokenLifecycleService {
        return AuthTokenLifecycleService(
            authRefreshTokenRepository = authRefreshTokenRepository,
            refreshTokenTtlSeconds = refreshTokenTtlSeconds,
            clock = clock
        )
    }

    @Bean
    fun authCommandService(
        userRepository: UserRepository,
        passwordHasher: PasswordHasher,
        jwtTokenService: JwtTokenService,
        authTokenLifecycleService: AuthTokenLifecycleService,
        userActionTokenRepository: UserActionTokenRepository,
        clock: Clock
    ): AuthCommandService {
        return AuthCommandService(
            userRepository = userRepository,
            passwordHasher = passwordHasher,
            jwtTokenService = jwtTokenService,
            authTokenLifecycleService = authTokenLifecycleService,
            userActionTokenRepository = userActionTokenRepository,
            clock = clock
        )
    }

    @Bean
    fun meService(
        userRepository: UserRepository,
        organizationQueryService: OrganizationQueryService,
        clock: Clock
    ): MeService {
        return MeService(
            userRepository = userRepository,
            organizationQueryService = organizationQueryService,
            clock = clock
        )
    }
}
