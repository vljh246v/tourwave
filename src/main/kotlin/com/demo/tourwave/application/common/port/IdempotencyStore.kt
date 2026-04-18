package com.demo.tourwave.application.common.port

interface IdempotencyStore {
    fun reserveOrReplay(
        actorUserId: Long,
        method: String,
        pathTemplate: String,
        idempotencyKey: String,
        requestHash: String,
    ): IdempotencyDecision

    fun complete(
        actorUserId: Long,
        method: String,
        pathTemplate: String,
        idempotencyKey: String,
        status: Int,
        body: Any,
    )

    fun markInProgressForTest(
        actorUserId: Long,
        method: String,
        pathTemplate: String,
        idempotencyKey: String,
        requestHash: String,
    )

    fun clear()
}

sealed interface IdempotencyDecision {
    data object Reserved : IdempotencyDecision

    data class Replay(
        val status: Int,
        val body: Any,
    ) : IdempotencyDecision
}

interface IdempotencyMaintenancePort {
    fun purgeExpired(nowEpochMillis: Long): Long
}
