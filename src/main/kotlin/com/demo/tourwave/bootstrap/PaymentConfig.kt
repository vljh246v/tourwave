package com.demo.tourwave.bootstrap

import com.demo.tourwave.application.booking.PaymentLedgerService
import com.demo.tourwave.application.booking.RefundRetryService
import com.demo.tourwave.application.booking.port.BookingRepository
import com.demo.tourwave.application.booking.port.PaymentRecordRepository
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
        @Value("\${tourwave.payment.webhook-secret:tourwave-webhook-secret}") webhookSecret: String,
        clock: Clock
    ): PaymentWebhookService {
        return PaymentWebhookService(
            paymentProviderEventRepository = paymentProviderEventRepository,
            bookingRepository = bookingRepository,
            paymentLedgerService = paymentLedgerService,
            webhookSecret = webhookSecret,
            clock = clock
        )
    }

    @Bean
    fun refundOperationsService(
        paymentRecordRepository: PaymentRecordRepository,
        bookingRepository: BookingRepository,
        refundRetryService: RefundRetryService
    ): RefundOperationsService {
        return RefundOperationsService(
            paymentRecordRepository = paymentRecordRepository,
            bookingRepository = bookingRepository,
            refundRetryService = refundRetryService
        )
    }

    @Bean
    fun reconciliationService(
        bookingRepository: BookingRepository,
        paymentRecordRepository: PaymentRecordRepository,
        paymentReconciliationSummaryRepository: PaymentReconciliationSummaryRepository,
        clock: Clock
    ): ReconciliationService {
        return ReconciliationService(
            bookingRepository = bookingRepository,
            paymentRecordRepository = paymentRecordRepository,
            paymentReconciliationSummaryRepository = paymentReconciliationSummaryRepository,
            clock = clock
        )
    }
}
