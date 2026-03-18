package com.demo.tourwave.application.common.port

interface AuditEventSubscriber {
    fun handle(event: AuditEventCommand)
}
