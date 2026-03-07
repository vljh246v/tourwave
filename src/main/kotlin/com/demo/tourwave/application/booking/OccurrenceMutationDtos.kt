package com.demo.tourwave.application.booking

data class CancelOccurrenceCommand(
    val occurrenceId: Long,
    val actorUserId: Long,
    val idempotencyKey: String,
    val requestId: String? = null
)

data class CancelOccurrenceResult(
    val status: Int
)

data class FinishOccurrenceCommand(
    val occurrenceId: Long,
    val actorUserId: Long,
    val idempotencyKey: String,
    val requestId: String? = null
)

data class FinishOccurrenceResult(
    val status: Int
)
