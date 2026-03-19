package com.demo.tourwave.application.payment.port

import com.demo.tourwave.domain.payment.PaymentProviderEvent
import java.time.Instant

interface PaymentProviderEventRepository {
    fun save(event: PaymentProviderEvent): PaymentProviderEvent
    fun findByProviderEventId(providerEventId: String): PaymentProviderEvent?
    fun findAll(): List<PaymentProviderEvent>
    fun findReceivedBetween(startInclusive: Instant, endExclusive: Instant): List<PaymentProviderEvent>
    fun clear()
}
