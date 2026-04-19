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
        assertions: (ConfigurableApplicationContext) -> Unit,
    ) {
        val context =
            SpringApplicationBuilder(TourwaveApplication::class.java)
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
            assertThat(context.environment.getProperty("integration.payment.provider-name"))
                .isEqualTo("stub-pay")
            assertThat(context.environment.getProperty("integration.notification.base-url"))
                .isEqualTo("http://localhost:18081/mock-notification")
            assertThat(context.environment.getProperty("integration.asset.base-url"))
                .isEqualTo("http://localhost:18082/mock-asset")
            assertThat(context.environment.getProperty("tourwave.app.base-url"))
                .isEqualTo("http://localhost:3000")
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
            "ALPHA_PAYMENT_PROVIDER_NAME=alpha-provider",
            "ALPHA_NOTIFICATION_BASE_URL=https://alpha-notification.internal",
            "ALPHA_NOTIFICATION_API_KEY=alpha-notification-key",
            "ALPHA_NOTIFICATION_SENDER_EMAIL=no-reply@alpha.internal",
            "ALPHA_ASSET_BASE_URL=https://alpha-asset.internal",
            "ALPHA_ASSET_PUBLIC_BASE_URL=https://cdn.alpha.internal",
            "ALPHA_ASSET_BUCKET=alpha-tourwave",
            "ALPHA_ASSET_ACCESS_KEY=alpha-asset-access",
            "ALPHA_ASSET_SECRET_KEY=alpha-asset-secret",
            "ALPHA_APP_BASE_URL=https://alpha-app.internal",
        ) { context ->
            assertThat(context.environment.getProperty("tourwave.environment")).isEqualTo("alpha")
            assertThat(context.environment.getProperty("integration.payment.api-key"))
                .isEqualTo("alpha-payment-key")
            assertThat(context.environment.getProperty("integration.payment.provider-name"))
                .isEqualTo("alpha-provider")
            assertThat(context.environment.getProperty("integration.notification.api-key"))
                .isEqualTo("alpha-notification-key")
            assertThat(context.environment.getProperty("integration.notification.sender-email"))
                .isEqualTo("no-reply@alpha.internal")
            assertThat(context.environment.getProperty("integration.asset.secret-key"))
                .isEqualTo("alpha-asset-secret")
            assertThat(context.environment.getProperty("tourwave.app.base-url"))
                .isEqualTo("https://alpha-app.internal")
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
            "BETA_PAYMENT_PROVIDER_NAME=beta-provider",
            "BETA_NOTIFICATION_BASE_URL=https://beta-notification.internal",
            "BETA_NOTIFICATION_API_KEY=beta-notification-key",
            "BETA_NOTIFICATION_SENDER_EMAIL=no-reply@beta.internal",
            "BETA_ASSET_BASE_URL=https://beta-asset.internal",
            "BETA_ASSET_PUBLIC_BASE_URL=https://cdn.beta.internal",
            "BETA_ASSET_BUCKET=beta-tourwave",
            "BETA_ASSET_ACCESS_KEY=beta-asset-access",
            "BETA_ASSET_SECRET_KEY=beta-asset-secret",
            "BETA_APP_BASE_URL=https://beta-app.internal",
        ) { context ->
            assertThat(context.environment.getProperty("tourwave.environment")).isEqualTo("beta")
            assertThat(context.environment.getProperty("integration.payment.api-key"))
                .isEqualTo("beta-payment-key")
            assertThat(context.environment.getProperty("integration.payment.provider-name"))
                .isEqualTo("beta-provider")
            assertThat(context.environment.getProperty("integration.notification.api-key"))
                .isEqualTo("beta-notification-key")
            assertThat(context.environment.getProperty("integration.notification.sender-email"))
                .isEqualTo("no-reply@beta.internal")
            assertThat(context.environment.getProperty("integration.asset.secret-key"))
                .isEqualTo("beta-asset-secret")
            assertThat(context.environment.getProperty("tourwave.app.base-url"))
                .isEqualTo("https://beta-app.internal")
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
            "REAL_PAYMENT_PROVIDER_NAME=real-provider",
            "REAL_NOTIFICATION_BASE_URL=https://real-notification.internal",
            "REAL_NOTIFICATION_API_KEY=real-notification-key",
            "REAL_NOTIFICATION_SENDER_EMAIL=no-reply@real.internal",
            "REAL_ASSET_BASE_URL=https://real-asset.internal",
            "REAL_ASSET_PUBLIC_BASE_URL=https://cdn.real.internal",
            "REAL_ASSET_BUCKET=real-tourwave",
            "REAL_ASSET_ACCESS_KEY=real-asset-access",
            "REAL_ASSET_SECRET_KEY=real-asset-secret",
            "REAL_APP_BASE_URL=https://app.tourwave.internal",
        ) { context ->
            assertThat(context.environment.getProperty("tourwave.environment")).isEqualTo("real")
            assertThat(context.environment.getProperty("tourwave.auth.allow-header-auth-fallback"))
                .isEqualTo("false")
            assertThat(context.environment.getProperty("integration.payment.api-key"))
                .isEqualTo("real-payment-key")
            assertThat(context.environment.getProperty("integration.payment.provider-name"))
                .isEqualTo("real-provider")
            assertThat(context.environment.getProperty("integration.notification.api-key"))
                .isEqualTo("real-notification-key")
            assertThat(context.environment.getProperty("integration.notification.sender-email"))
                .isEqualTo("no-reply@real.internal")
            assertThat(context.environment.getProperty("integration.asset.secret-key"))
                .isEqualTo("real-asset-secret")
            assertThat(context.environment.getProperty("tourwave.app.base-url"))
                .isEqualTo("https://app.tourwave.internal")
        }
    }
}
