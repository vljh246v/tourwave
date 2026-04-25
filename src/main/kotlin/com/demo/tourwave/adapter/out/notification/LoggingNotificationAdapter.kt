package com.demo.tourwave.adapter.out.notification

import com.demo.tourwave.application.auth.NotificationChannelPort
import org.slf4j.LoggerFactory

class LoggingNotificationAdapter : NotificationChannelPort {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun send(
        recipient: String,
        subject: String,
        body: String,
        templateType: String,
    ) {
        logger.info(
            "Email notification (stub): recipient={}, subject={}, templateType={}",
            maskEmail(recipient),
            subject,
            templateType,
        )
    }

    private fun maskEmail(email: String): String {
        val atIndex = email.indexOf('@')
        if (atIndex <= 0) return "****"
        return email[0] + "****" + email.substring(atIndex)
    }
}
