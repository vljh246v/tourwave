package com.demo.tourwave.support

import com.demo.tourwave.application.auth.NotificationChannelPort

class FakeNotificationChannelPort : NotificationChannelPort {
    data class SentMessage(
        val recipient: String,
        val subject: String,
        val body: String,
        val templateType: String,
    )

    val sentMessages = mutableListOf<SentMessage>()
    var shouldThrow: Exception? = null

    override fun send(
        recipient: String,
        subject: String,
        body: String,
        templateType: String,
    ) {
        shouldThrow?.let { throw it }
        sentMessages.add(SentMessage(recipient, subject, body, templateType))
    }

    fun clear() {
        sentMessages.clear()
        shouldThrow = null
    }
}
