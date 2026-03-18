package com.demo.tourwave.bootstrap

import com.demo.tourwave.application.booking.PaymentLedgerService
import com.demo.tourwave.application.booking.RefundRetryService
import com.demo.tourwave.application.booking.port.BookingRepository
import com.demo.tourwave.application.booking.port.PaymentRecordRepository
import com.demo.tourwave.application.common.port.AuditEventPort
import com.demo.tourwave.application.payment.PaymentWebhookService
import com.demo.tourwave.application.payment.ReconciliationService
import com.demo.tourwave.application.payment.RefundOperationsService
import com.demo.tourwave.application.payment.port.PaymentProviderEventRepository
import com.demo.tourwave.application.payment.port.PaymentReconciliationSummaryRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock

@Configuration
class PaymentConfig {
    @Bean
    fun paymentWebhookService(
        paymentProviderEventRepository: PaymentProviderEventRepository,
        bookingRepository: BookingRepository,
        paymentLedgerService: PaymentLedgerService,
        @Value("\${tourwave.payment.webhook-secrets:}") webhookSecretsRaw: String,
        @Value("\${tourwave.payment.webhook-secret:tourwave-webhook-secret}") webhookSecret: String,
        clock: Clock
    ): PaymentWebhookService {
        return PaymentWebhookService(
            paymentProviderEventRepository = paymentProviderEventRepository,
            bookingRepository = bookingRepository,
            paymentLedgerService = paymentLedgerService,
            webhookSecrets = webhookSecrets(webhookSecretsRaw, webhookSecret),
            clock = clock
        )
    }

    @Bean
    fun refundOperationsService(
        paymentRecordRepository: PaymentRecordRepository,
        bookingRepository: BookingRepository,
        refundRetryService: RefundRetryService,
        auditEventPort: AuditEventPort,
        @Value("\${tourwave.payment.refund.max-retry-attempts:5}") maxRetryAttempts: Int,
        @Value("\${tourwave.payment.refund.retry-cooldown-seconds:600}") retryCooldownSeconds: Long,
        clock: Clock
    ): RefundOperationsService {
        return RefundOperationsService(
            paymentRecordRepository = paymentRecordRepository,
            bookingRepository = bookingRepository,
            refundRetryService = refundRetryService,
            auditEventPort = auditEventPort,
            maxRetryAttempts = maxRetryAttempts,
            retryCooldown = java.time.Duration.ofSeconds(retryCooldownSeconds),
            clock = clock
        )
    }

    @Bean
    fun reconciliationService(
        bookingRepository: BookingRepository,
        paymentRecordRepository: PaymentRecordRepository,
        paymentProviderEventRepository: PaymentProviderEventRepository,
        paymentReconciliationSummaryRepository: PaymentReconciliationSummaryRepository,
        clock: Clock
    ): ReconciliationService {
        return ReconciliationService(
            bookingRepository = bookingRepository,
            paymentRecordRepository = paymentRecordRepository,
            paymentProviderEventRepository = paymentProviderEventRepository,
            paymentReconciliationSummaryRepository = paymentReconciliationSummaryRepository,
            clock = clock
        )
    }

    private fun webhookSecrets(webhookSecretsRaw: String, webhookSecret: String): List<String> {
        return webhookSecretsRaw.split(',')
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .ifEmpty { listOf(webhookSecret) }
    }
}
