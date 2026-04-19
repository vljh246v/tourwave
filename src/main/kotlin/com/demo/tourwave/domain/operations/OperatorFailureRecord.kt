package com.demo.tourwave.domain.operations

import java.time.Instant

enum class OperatorFailureSourceType {
    REFUND,
    NOTIFICATION_DELIVERY,
    PAYMENT_WEBHOOK,
}

enum class OperatorFailureRecordStatus {
    OPEN,
    RESOLVED,
}

enum class OperatorFailureAction {
    RETRY,
    RESOLVE,
}

data class OperatorFailureRecord(
    val id: Long? = null,
    val sourceType: OperatorFailureSourceType,
    val sourceKey: String,
    val status: OperatorFailureRecordStatus = OperatorFailureRecordStatus.OPEN,
    val lastAction: OperatorFailureAction,
    val note: String? = null,
    val lastActionByUserId: Long,
    val lastActionAtUtc: Instant,
    val retryCount: Int = 0,
    val createdAtUtc: Instant = lastActionAtUtc,
    val updatedAtUtc: Instant = lastActionAtUtc,
)
