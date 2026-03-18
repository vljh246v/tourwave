package com.demo.tourwave.adapter.out.persistence.customer

import com.demo.tourwave.application.customer.port.NotificationChannelException
import com.demo.tourwave.application.customer.port.NotificationChannelPort
import com.demo.tourwave.application.customer.port.NotificationChannelSendResult
import com.demo.tourwave.application.customer.port.SendNotificationMessage
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

@Component
@Profile("alpha", "beta", "real")
class HttpEmailNotificationChannelAdapter(
    @Value("\${integration.notification.base-url}") private val baseUrl: String,
    @Value("\${integration.notification.api-key}") private val apiKey: String,
    @Value("\${integration.notification.sender-email}") private val senderEmail: String,
    @Value("\${integration.notification.sender-name:Tourwave}") private val senderName: String,
    private val objectMapper: ObjectMapper
) : NotificationChannelPort {
    private val httpClient = HttpClient.newBuilder().build()

    override fun send(message: SendNotificationMessage): NotificationChannelSendResult {
        val payload = mapOf(
            "channel" to message.channel.name,
            "to" to message.recipient,
            "subject" to message.subject,
            "body" to message.body,
            "templateCode" to message.templateCode,
            "idempotencyKey" to message.idempotencyKey,
            "fromEmail" to senderEmail,
            "fromName" to senderName
        )
        val response = httpClient.send(
            HttpRequest.newBuilder(URI.create("${baseUrl.trimEnd('/')}/deliveries/email"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer $apiKey")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                .build(),
            HttpResponse.BodyHandlers.ofString()
        )
        if (response.statusCode() in 200..299) {
            val providerMessageId = objectMapper.readTree(response.body()).path("messageId").takeIf { !it.isMissingNode && !it.isNull }?.asText()
            return NotificationChannelSendResult(providerMessageId)
        }
        if (response.statusCode() == 429 || response.statusCode() >= 500) {
            throw NotificationChannelException("notification provider temporary failure", retryable = true)
        }
        throw NotificationChannelException("notification provider rejected message", retryable = false)
    }
}
