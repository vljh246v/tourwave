package com.demo.tourwave.adapter.out.persistence.payment

import com.demo.tourwave.application.payment.port.PaymentProviderEventRepository
import com.demo.tourwave.domain.payment.PaymentProviderEvent
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

@Repository
@Profile("!mysql & !mysql-test")
class InMemoryPaymentProviderEventRepositoryAdapter : PaymentProviderEventRepository {
    private val sequence = AtomicLong(0)
    private val eventsById = ConcurrentHashMap<Long, PaymentProviderEvent>()
    private val eventIdIndex = ConcurrentHashMap<String, Long>()

    override fun save(event: PaymentProviderEvent): PaymentProviderEvent {
        val id = event.id ?: sequence.incrementAndGet()
        val saved = event.copy(id = id)
        eventsById[id] = saved
        eventIdIndex[saved.providerEventId] = id
        return saved
    }

    override fun findByProviderEventId(providerEventId: String): PaymentProviderEvent? {
        val id = eventIdIndex[providerEventId] ?: return null
        return eventsById[id]
    }

    override fun findReceivedBetween(startInclusive: Instant, endExclusive: Instant): List<PaymentProviderEvent> {
        return eventsById.values
            .filter { !it.receivedAtUtc.isBefore(startInclusive) && it.receivedAtUtc.isBefore(endExclusive) }
            .sortedBy { it.receivedAtUtc }
    }

    override fun clear() {
        eventsById.clear()
        eventIdIndex.clear()
        sequence.set(0)
    }
}
