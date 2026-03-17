package com.demo.tourwave

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.WebApplicationType
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.context.ConfigurableApplicationContext

class ProfileConfigurationTest {

    private fun runWithProfile(
        profile: String,
        vararg properties: String,
        assertions: (ConfigurableApplicationContext) -> Unit
    ) {
        val context = SpringApplicationBuilder(TourwaveApplication::class.java)
            .web(WebApplicationType.NONE)
            .profiles(profile)
            .properties(*properties)
            .run()

        try {
            assertions(context)
        } finally {
            context.close()
        }
    }

    @Test
    fun `local profile loads with localhost defaults`() {
        runWithProfile("local") { context ->
            assertThat(context.environment.getProperty("tourwave.environment")).isEqualTo("local")
            assertThat(context.environment.getProperty("tourwave.auth.allow-header-auth-fallback"))
                .isEqualTo("true")
            assertThat(context.environment.getProperty("integration.payment.base-url"))
                .isEqualTo("http://localhost:18080/mock-payment")
            assertThat(context.environment.getProperty("integration.notification.base-url"))
                .isEqualTo("http://localhost:18081/mock-notification")
            assertThat(context.environment.getProperty("integration.asset.base-url"))
                .isEqualTo("http://localhost:18082/mock-asset")
        }
    }

    @Test
    fun `alpha profile loads with env mapped values`() {
        runWithProfile(
            "alpha",
                "ALPHA_DB_URL=jdbc:mysql://alpha-db:3306/tourwave_alpha",
                "ALPHA_DB_USERNAME=alpha_user",
                "ALPHA_DB_PASSWORD=alpha_password",
                "ALPHA_PAYMENT_BASE_URL=https://alpha-payment.internal",
                "ALPHA_PAYMENT_API_KEY=alpha-payment-key",
                "ALPHA_NOTIFICATION_BASE_URL=https://alpha-notification.internal",
                "ALPHA_NOTIFICATION_API_KEY=alpha-notification-key",
                "ALPHA_ASSET_BASE_URL=https://alpha-asset.internal",
                "ALPHA_ASSET_ACCESS_TOKEN=alpha-asset-token"
        ) { context ->
            assertThat(context.environment.getProperty("tourwave.environment")).isEqualTo("alpha")
            assertThat(context.environment.getProperty("integration.payment.api-key"))
                .isEqualTo("alpha-payment-key")
            assertThat(context.environment.getProperty("integration.notification.api-key"))
                .isEqualTo("alpha-notification-key")
            assertThat(context.environment.getProperty("integration.asset.access-token"))
                .isEqualTo("alpha-asset-token")
        }
    }

    @Test
    fun `beta profile loads with env mapped values`() {
        runWithProfile(
            "beta",
                "BETA_DB_URL=jdbc:mysql://beta-db:3306/tourwave_beta",
                "BETA_DB_USERNAME=beta_user",
                "BETA_DB_PASSWORD=beta_password",
                "BETA_PAYMENT_BASE_URL=https://beta-payment.internal",
                "BETA_PAYMENT_API_KEY=beta-payment-key",
                "BETA_NOTIFICATION_BASE_URL=https://beta-notification.internal",
                "BETA_NOTIFICATION_API_KEY=beta-notification-key",
                "BETA_ASSET_BASE_URL=https://beta-asset.internal",
                "BETA_ASSET_ACCESS_TOKEN=beta-asset-token"
        ) { context ->
            assertThat(context.environment.getProperty("tourwave.environment")).isEqualTo("beta")
            assertThat(context.environment.getProperty("integration.payment.api-key"))
                .isEqualTo("beta-payment-key")
            assertThat(context.environment.getProperty("integration.notification.api-key"))
                .isEqualTo("beta-notification-key")
            assertThat(context.environment.getProperty("integration.asset.access-token"))
                .isEqualTo("beta-asset-token")
        }
    }

    @Test
    fun `real profile loads with env mapped values`() {
        runWithProfile(
            "real",
                "REAL_DB_URL=jdbc:mysql://real-db:3306/tourwave_real",
                "REAL_DB_USERNAME=real_user",
                "REAL_DB_PASSWORD=real_password",
                "REAL_PAYMENT_BASE_URL=https://real-payment.internal",
                "REAL_PAYMENT_API_KEY=real-payment-key",
                "REAL_NOTIFICATION_BASE_URL=https://real-notification.internal",
                "REAL_NOTIFICATION_API_KEY=real-notification-key",
                "REAL_ASSET_BASE_URL=https://real-asset.internal",
                "REAL_ASSET_ACCESS_TOKEN=real-asset-token"
        ) { context ->
            assertThat(context.environment.getProperty("tourwave.environment")).isEqualTo("real")
            assertThat(context.environment.getProperty("tourwave.auth.allow-header-auth-fallback"))
                .isEqualTo("false")
            assertThat(context.environment.getProperty("integration.payment.api-key"))
                .isEqualTo("real-payment-key")
            assertThat(context.environment.getProperty("integration.notification.api-key"))
                .isEqualTo("real-notification-key")
            assertThat(context.environment.getProperty("integration.asset.access-token"))
                .isEqualTo("real-asset-token")
        }
    }
}
