package com.demo.tourwave.application.auth

interface NotificationChannelPort {
    fun send(
        recipient: String,
        subject: String,
        body: String,
        templateType: String,
    )
}
