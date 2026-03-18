package com.demo.tourwave.adapter.out.persistence.audit

import com.demo.tourwave.application.common.port.AuditEventCommand
import com.demo.tourwave.application.common.port.AuditEventPort
import com.demo.tourwave.application.common.port.AuditEventSubscriber
import org.springframework.stereotype.Component
import java.util.concurrent.CopyOnWriteArrayList

@Component
class InMemoryAuditEventAdapter(
    private val subscribers: List<AuditEventSubscriber> = emptyList()
) : AuditEventPort {
    private val events = CopyOnWriteArrayList<AuditEventCommand>()

    override fun append(event: AuditEventCommand) {
        events.add(event)
        subscribers.forEach { it.handle(event) }
    }

    fun all(): List<AuditEventCommand> {
        return events.toList()
    }

    fun clear() {
        events.clear()
    }
}
