package com.demo.tourwave.application.operations

import com.demo.tourwave.application.booking.port.PaymentRecordRepository
import com.demo.tourwave.application.common.port.AuditEventCommand
import com.demo.tourwave.application.common.port.AuditEventPort
import com.demo.tourwave.application.customer.NotificationDeliveryService
import com.demo.tourwave.application.customer.port.NotificationDeliveryRepository
import com.demo.tourwave.application.operations.port.OperatorFailureRecordRepository
import com.demo.tourwave.application.payment.PaymentWebhookService
import com.demo.tourwave.application.payment.RefundOperationsService
import com.demo.tourwave.application.payment.port.PaymentProviderEventRepository
import com.demo.tourwave.domain.customer.NotificationDeliveryStatus
import com.demo.tourwave.domain.operations.OperatorFailureAction
import com.demo.tourwave.domain.operations.OperatorFailureRecord
import com.demo.tourwave.domain.operations.OperatorFailureRecordStatus
import com.demo.tourwave.domain.operations.OperatorFailureSourceType
import com.demo.tourwave.domain.payment.PaymentProviderEventStatus
import com.demo.tourwave.domain.payment.PaymentRecordStatus
import java.time.Clock
import java.time.Instant

enum class OperatorRemediationAction {
    RETRY,
    RESOLVE,
}

data class OperatorRemediationCommand(
    val actorUserId: Long,
    val action: OperatorRemediationAction,
    val note: String? = null,
)

data class OperatorRemediationQueueItem(
    val sourceType: OperatorFailureSourceType,
    val sourceKey: String,
    val queueStatus: OperatorFailureRecordStatus,
    val failureCategory: String,
    val resourceType: String,
    val resourceId: Long?,
    val detail: String,
    val retryable: Boolean,
    val availableActions: Set<OperatorRemediationAction>,
    val sourceOccurredAtUtc: Instant,
    val sourceUpdatedAtUtc: Instant,
    val actionRetryCount: Int,
    val lastQueueAction: OperatorFailureAction?,
    val lastActionByUserId: Long?,
    val lastActionAtUtc: Instant?,
    val lastActionNote: String?,
)

class OperatorRemediationQueueService(
    private val paymentRecordRepository: PaymentRecordRepository,
    private val refundOperationsService: RefundOperationsService,
    private val notificationDeliveryRepository: NotificationDeliveryRepository,
    private val notificationDeliveryService: NotificationDeliveryService,
    private val paymentProviderEventRepository: PaymentProviderEventRepository,
    private val paymentWebhookService: PaymentWebhookService,
    private val operatorFailureRecordRepository: OperatorFailureRecordRepository,
    private val auditEventPort: AuditEventPort,
    private val clock: Clock,
) {
    fun listOpenItems(): List<OperatorRemediationQueueItem> {
        return buildRawItems()
            .mapNotNull { raw -> mergeWithRecord(raw) }
            .sortedByDescending { it.sourceUpdatedAtUtc }
    }

    fun remediate(
        sourceType: OperatorFailureSourceType,
        sourceKey: String,
        command: OperatorRemediationCommand,
    ): OperatorRemediationQueueItem {
        val rawItem =
            buildRawItems().firstOrNull { it.sourceType == sourceType && it.sourceKey == sourceKey }
                ?: throw IllegalArgumentException("Remediation queue item not found: $sourceType/$sourceKey")
        val now = clock.instant()
        return when (command.action) {
            OperatorRemediationAction.RETRY -> {
                require(rawItem.retryable) { "Retry is not supported for $sourceType/$sourceKey" }
                when (sourceType) {
                    OperatorFailureSourceType.REFUND ->
                        refundOperationsService.remediateBookingRefund(
                            sourceKey.toLong(),
                            com.demo.tourwave.application.payment.RefundRemediationCommand(actorUserId = command.actorUserId),
                        )

                    OperatorFailureSourceType.NOTIFICATION_DELIVERY ->
                        notificationDeliveryService.redeliver(sourceKey.toLong())

                    OperatorFailureSourceType.PAYMENT_WEBHOOK ->
                        paymentWebhookService.reprocessPoisonedEvent(sourceKey)
                }
                val record = upsertRecord(rawItem, command.actorUserId, OperatorFailureAction.RETRY, command.note, now)
                appendAudit(rawItem, record, command.actorUserId, command.note)
                listOpenItems().firstOrNull { it.sourceType == sourceType && it.sourceKey == sourceKey }
                    ?: rawItem.copy(
                        queueStatus = record.status,
                        actionRetryCount = record.retryCount,
                        lastQueueAction = record.lastAction,
                        lastActionByUserId = record.lastActionByUserId,
                        lastActionAtUtc = record.lastActionAtUtc,
                        lastActionNote = record.note,
                    )
            }

            OperatorRemediationAction.RESOLVE -> {
                val record =
                    upsertRecord(
                        rawItem = rawItem,
                        actorUserId = command.actorUserId,
                        action = OperatorFailureAction.RESOLVE,
                        note = command.note,
                        now = now,
                        status = OperatorFailureRecordStatus.RESOLVED,
                    )
                appendAudit(rawItem, record, command.actorUserId, command.note)
                rawItem.copy(
                    queueStatus = record.status,
                    actionRetryCount = record.retryCount,
                    lastQueueAction = record.lastAction,
                    lastActionByUserId = record.lastActionByUserId,
                    lastActionAtUtc = record.lastActionAtUtc,
                    lastActionNote = record.note,
                )
            }
        }
    }

    private fun buildRawItems(): List<OperatorRemediationQueueItem> {
        val refundItems =
            paymentRecordRepository.findByStatuses(
                setOf(
                    PaymentRecordStatus.REFUND_FAILED_RETRYABLE,
                    PaymentRecordStatus.REFUND_REVIEW_REQUIRED,
                ),
            ).map {
                OperatorRemediationQueueItem(
                    sourceType = OperatorFailureSourceType.REFUND,
                    sourceKey = it.bookingId.toString(),
                    queueStatus = OperatorFailureRecordStatus.OPEN,
                    failureCategory = "PAYMENT_REFUND",
                    resourceType = "BOOKING",
                    resourceId = it.bookingId,
                    detail = it.lastErrorCode ?: it.status.name,
                    retryable = it.status == PaymentRecordStatus.REFUND_FAILED_RETRYABLE,
                    availableActions =
                        if (it.status == PaymentRecordStatus.REFUND_FAILED_RETRYABLE) {
                            setOf(
                                OperatorRemediationAction.RETRY,
                                OperatorRemediationAction.RESOLVE,
                            )
                        } else {
                            setOf(OperatorRemediationAction.RESOLVE)
                        },
                    sourceOccurredAtUtc = it.lastRefundAttemptedAtUtc ?: it.updatedAtUtc,
                    sourceUpdatedAtUtc = it.updatedAtUtc,
                    actionRetryCount = 0,
                    lastQueueAction = null,
                    lastActionByUserId = null,
                    lastActionAtUtc = null,
                    lastActionNote = null,
                )
            }
        val notificationItems =
            notificationDeliveryRepository.findAll()
                .filter { it.status == NotificationDeliveryStatus.FAILED_RETRYABLE || it.status == NotificationDeliveryStatus.FAILED_PERMANENT }
                .map {
                    OperatorRemediationQueueItem(
                        sourceType = OperatorFailureSourceType.NOTIFICATION_DELIVERY,
                        sourceKey = requireNotNull(it.id).toString(),
                        queueStatus = OperatorFailureRecordStatus.OPEN,
                        failureCategory = "NOTIFICATION_DELIVERY",
                        resourceType = it.resourceType,
                        resourceId = it.resourceId,
                        detail = it.lastError ?: it.status.name,
                        retryable = it.status == NotificationDeliveryStatus.FAILED_RETRYABLE,
                        availableActions =
                            if (it.status == NotificationDeliveryStatus.FAILED_RETRYABLE) {
                                setOf(
                                    OperatorRemediationAction.RETRY,
                                    OperatorRemediationAction.RESOLVE,
                                )
                            } else {
                                setOf(OperatorRemediationAction.RESOLVE)
                            },
                        sourceOccurredAtUtc = it.updatedAt,
                        sourceUpdatedAtUtc = it.updatedAt,
                        actionRetryCount = 0,
                        lastQueueAction = null,
                        lastActionByUserId = null,
                        lastActionAtUtc = null,
                        lastActionNote = null,
                    )
                }
        val webhookItems =
            paymentProviderEventRepository.findAll()
                .filter {
                    it.status == PaymentProviderEventStatus.REJECTED_SIGNATURE ||
                        it.status == PaymentProviderEventStatus.MALFORMED_PAYLOAD ||
                        it.status == PaymentProviderEventStatus.POISONED
                }.map {
                    OperatorRemediationQueueItem(
                        sourceType = OperatorFailureSourceType.PAYMENT_WEBHOOK,
                        sourceKey = it.providerEventId,
                        queueStatus = OperatorFailureRecordStatus.OPEN,
                        failureCategory = "PAYMENT_WEBHOOK",
                        resourceType = "PAYMENT_PROVIDER_EVENT",
                        resourceId = it.id,
                        detail = it.note ?: it.status.name,
                        retryable = it.status == PaymentProviderEventStatus.POISONED,
                        availableActions =
                            if (it.status == PaymentProviderEventStatus.POISONED) {
                                setOf(
                                    OperatorRemediationAction.RETRY,
                                    OperatorRemediationAction.RESOLVE,
                                )
                            } else {
                                setOf(OperatorRemediationAction.RESOLVE)
                            },
                        sourceOccurredAtUtc = it.receivedAtUtc,
                        sourceUpdatedAtUtc = it.processedAtUtc ?: it.receivedAtUtc,
                        actionRetryCount = 0,
                        lastQueueAction = null,
                        lastActionByUserId = null,
                        lastActionAtUtc = null,
                        lastActionNote = null,
                    )
                }
        return refundItems + notificationItems + webhookItems
    }

    private fun mergeWithRecord(rawItem: OperatorRemediationQueueItem): OperatorRemediationQueueItem? {
        val record = operatorFailureRecordRepository.findBySource(rawItem.sourceType, rawItem.sourceKey)
        if (record != null && record.status == OperatorFailureRecordStatus.RESOLVED && !rawItem.sourceUpdatedAtUtc.isAfter(record.updatedAtUtc)) {
            return null
        }
        return rawItem.copy(
            queueStatus = record?.status ?: OperatorFailureRecordStatus.OPEN,
            actionRetryCount = record?.retryCount ?: 0,
            lastQueueAction = record?.lastAction,
            lastActionByUserId = record?.lastActionByUserId,
            lastActionAtUtc = record?.lastActionAtUtc,
            lastActionNote = record?.note,
        )
    }

    private fun upsertRecord(
        rawItem: OperatorRemediationQueueItem,
        actorUserId: Long,
        action: OperatorFailureAction,
        note: String?,
        now: Instant,
        status: OperatorFailureRecordStatus = OperatorFailureRecordStatus.OPEN,
    ): OperatorFailureRecord {
        val current = operatorFailureRecordRepository.findBySource(rawItem.sourceType, rawItem.sourceKey)
        return operatorFailureRecordRepository.save(
            OperatorFailureRecord(
                id = current?.id,
                sourceType = rawItem.sourceType,
                sourceKey = rawItem.sourceKey,
                status = status,
                lastAction = action,
                note = note,
                lastActionByUserId = actorUserId,
                lastActionAtUtc = now,
                retryCount = if (action == OperatorFailureAction.RETRY) (current?.retryCount ?: 0) + 1 else current?.retryCount ?: 0,
                createdAtUtc = current?.createdAtUtc ?: now,
                updatedAtUtc = now,
            ),
        )
    }

    private fun appendAudit(
        rawItem: OperatorRemediationQueueItem,
        record: OperatorFailureRecord,
        actorUserId: Long,
        note: String?,
    ) {
        auditEventPort.append(
            AuditEventCommand(
                actor = "OPERATOR:$actorUserId",
                action = "OPERATOR_FAILURE_${record.lastAction.name}",
                resourceType = "OPERATOR_FAILURE_QUEUE",
                resourceId = requireNotNull(record.id ?: rawItem.resourceId ?: 0L),
                occurredAtUtc = clock.instant(),
                details =
                    mapOf(
                        "sourceType" to rawItem.sourceType.name,
                        "sourceKey" to rawItem.sourceKey,
                        "failureCategory" to rawItem.failureCategory,
                        "note" to note,
                    ),
                beforeJson =
                    mapOf(
                        "queueStatus" to rawItem.queueStatus.name,
                        "retryCount" to rawItem.actionRetryCount,
                    ),
                afterJson =
                    mapOf(
                        "queueStatus" to record.status.name,
                        "retryCount" to record.retryCount,
                    ),
            ),
        )
    }
}
