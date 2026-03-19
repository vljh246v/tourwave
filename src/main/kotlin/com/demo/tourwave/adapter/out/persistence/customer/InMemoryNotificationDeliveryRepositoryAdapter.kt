package com.demo.tourwave.adapter.out.persistence.customer

import com.demo.tourwave.application.customer.port.NotificationDeliveryRepository
import com.demo.tourwave.domain.customer.NotificationDelivery
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

@Component
@Profile("!mysql & !mysql-test")
class InMemoryNotificationDeliveryRepositoryAdapter : NotificationDeliveryRepository {
    private val sequence = AtomicLong(1)
    private val deliveries = ConcurrentHashMap<Long, NotificationDelivery>()

    override fun save(delivery: NotificationDelivery): NotificationDelivery {
        val id = delivery.id ?: sequence.getAndIncrement()
        val saved = delivery.copy(id = id)
        deliveries[id] = saved
        return saved
    }

    override fun findById(id: Long): NotificationDelivery? = deliveries[id]

    override fun findAll(): List<NotificationDelivery> = deliveries.values.sortedByDescending { it.createdAt }

    override fun clear() {
        deliveries.clear()
        sequence.set(1)
    }
}
