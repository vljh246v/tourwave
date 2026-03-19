package com.demo.tourwave.adapter.out.persistence.jpa.payment

import com.demo.tourwave.application.payment.port.PaymentProviderEventRepository
import com.demo.tourwave.domain.payment.PaymentProviderEvent
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
@Profile("mysql", "mysql-test")
class JpaPaymentProviderEventRepositoryAdapter(
    private val paymentProviderEventJpaRepository: PaymentProviderEventJpaRepository
) : PaymentProviderEventRepository {
    override fun save(event: PaymentProviderEvent): PaymentProviderEvent =
        paymentProviderEventJpaRepository.save(event.toEntity()).toDomain()

    override fun findByProviderEventId(providerEventId: String): PaymentProviderEvent? =
        paymentProviderEventJpaRepository.findByProviderEventId(providerEventId)?.toDomain()

    override fun findAll(): List<PaymentProviderEvent> =
        paymentProviderEventJpaRepository.findAllByOrderByReceivedAtUtcDesc().map { it.toDomain() }

    override fun findReceivedBetween(startInclusive: Instant, endExclusive: Instant): List<PaymentProviderEvent> =
        paymentProviderEventJpaRepository
            .findByReceivedAtUtcGreaterThanEqualAndReceivedAtUtcLessThanOrderByReceivedAtUtcAsc(startInclusive, endExclusive)
            .map { it.toDomain() }

    override fun clear() {
        paymentProviderEventJpaRepository.deleteAllInBatch()
    }
}

private fun PaymentProviderEvent.toEntity(): PaymentProviderEventJpaEntity =
    PaymentProviderEventJpaEntity(
        id = id,
        providerName = providerName,
        providerEventId = providerEventId,
        eventType = eventType,
        bookingId = bookingId,
        payloadJson = payloadJson,
        signature = signature,
        signatureKeyId = signatureKeyId,
        payloadSha256 = payloadSha256,
        status = status,
        note = note,
        receivedAtUtc = receivedAtUtc,
        processedAtUtc = processedAtUtc
    )

private fun PaymentProviderEventJpaEntity.toDomain(): PaymentProviderEvent =
    PaymentProviderEvent(
        id = id,
        providerName = providerName,
        providerEventId = providerEventId,
        eventType = eventType,
        bookingId = bookingId,
        payloadJson = payloadJson,
        signature = signature,
        signatureKeyId = signatureKeyId,
        payloadSha256 = payloadSha256,
        status = status,
        note = note,
        receivedAtUtc = receivedAtUtc,
        processedAtUtc = processedAtUtc
    )
