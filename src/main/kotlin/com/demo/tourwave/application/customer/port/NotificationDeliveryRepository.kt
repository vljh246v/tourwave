package com.demo.tourwave.application.customer.port

import com.demo.tourwave.domain.customer.NotificationDelivery

interface NotificationDeliveryRepository {
    fun save(delivery: NotificationDelivery): NotificationDelivery

    fun findById(id: Long): NotificationDelivery?

    fun findAll(): List<NotificationDelivery>

    fun clear()
}
