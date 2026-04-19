package com.demo.tourwave.bootstrap

import com.demo.tourwave.application.common.port.AuditEventPort
import com.demo.tourwave.application.user.UserPort
import com.demo.tourwave.application.user.UserService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock

@Configuration
class UserConfig {
    @Bean
    fun userService(
        userPort: UserPort,
        auditEventPort: AuditEventPort,
        clock: Clock,
    ): UserService {
        return UserService(
            userPort = userPort,
            auditEventPort = auditEventPort,
            clock = clock,
        )
    }
}
