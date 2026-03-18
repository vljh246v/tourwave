package com.demo.tourwave.adapter.out.persistence.customer

import com.demo.tourwave.application.customer.port.NotificationChannelPort
import com.demo.tourwave.application.customer.port.NotificationChannelSendResult
import com.demo.tourwave.application.customer.port.SendNotificationMessage
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.util.UUID

@Component
@Profile("!alpha & !beta & !real")
class FakeEmailNotificationChannelAdapter : NotificationChannelPort {
    override fun send(message: SendNotificationMessage): NotificationChannelSendResult {
        return NotificationChannelSendResult(providerMessageId = "fake-${UUID.randomUUID()}")
    }
}
