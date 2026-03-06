package com.demo.tourwave.domain.common

interface IdempotencyStore {
    fun reserveOrReplay(
        actorUserId: Long,
        method: String,
        pathTemplate: String,
        idempotencyKey: String,
        requestHash: String
    ): IdempotencyDecision

    fun complete(
        actorUserId: Long,
        method: String,
        pathTemplate: String,
        idempotencyKey: String,
        status: Int,
        body: Any
    )

    fun markInProgressForTest(
        actorUserId: Long,
        method: String,
        pathTemplate: String,
        idempotencyKey: String,
        requestHash: String
    )

    fun clear()
}

sealed interface IdempotencyDecision {
    data object Reserved : IdempotencyDecision
    data class Replay(
        val status: Int,
        val body: Any
    ) : IdempotencyDecision
}
