package com.demo.tourwave.adapter.out.persistence.jpa.payment

import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant

interface PaymentProviderEventJpaRepository : JpaRepository<PaymentProviderEventJpaEntity, Long> {
    fun findByProviderEventId(providerEventId: String): PaymentProviderEventJpaEntity?
    fun findByReceivedAtUtcGreaterThanEqualAndReceivedAtUtcLessThanOrderByReceivedAtUtcAsc(
        startInclusive: Instant,
        endExclusive: Instant
    ): List<PaymentProviderEventJpaEntity>
}
