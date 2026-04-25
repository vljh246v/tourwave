package com.demo.tourwave.support

import com.demo.tourwave.application.common.port.AuditEventCommand
import com.demo.tourwave.application.common.port.AuditEventPort

class FakeAuditEventPort : AuditEventPort {
    val events = mutableListOf<AuditEventCommand>()

    override fun append(event: AuditEventCommand) {
        events.add(event)
    }

    fun clear() {
        events.clear()
    }
}
