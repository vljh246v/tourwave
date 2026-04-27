package com.demo.tourwave.adapter.`in`.web.operations

import com.demo.tourwave.application.common.port.AuthzGuardPort
import com.demo.tourwave.application.operations.OperatorRemediationAction
import com.demo.tourwave.application.operations.OperatorRemediationCommand
import com.demo.tourwave.application.operations.OperatorRemediationQueueService
import com.demo.tourwave.domain.operations.OperatorFailureSourceType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController

@RestController
class OperatorRemediationQueueController(
    private val operatorRemediationQueueService: OperatorRemediationQueueService,
    private val authzGuardPort: AuthzGuardPort,
) {
    @GetMapping("/operator/operations/remediation-queue")
    fun listQueue(
        @RequestHeader("X-Actor-User-Id", required = false) actorUserId: Long?,
    ): List<OperatorRemediationQueueItemResponse> {
        authzGuardPort.requireActorUserId(actorUserId)
        return operatorRemediationQueueService.listOpenItems().map { it.toResponse() }
    }

    @PostMapping("/operator/operations/remediation-queue/{sourceType}/{sourceKey}")
    fun remediate(
        @PathVariable sourceType: OperatorFailureSourceType,
        @PathVariable sourceKey: String,
        @RequestHeader("X-Actor-User-Id", required = false) actorUserId: Long?,
        @RequestHeader("Idempotency-Key") idempotencyKey: String,
        @RequestBody request: OperatorRemediationRequest,
    ): OperatorRemediationQueueItemResponse {
        val requiredActorUserId = authzGuardPort.requireActorUserId(actorUserId)
        return operatorRemediationQueueService.remediate(
            sourceType = sourceType,
            sourceKey = sourceKey,
            command =
                OperatorRemediationCommand(
                    actorUserId = requiredActorUserId,
                    action = request.action,
                    note = request.note,
                    idempotencyKey = idempotencyKey,
                ),
        ).toResponse()
    }
}

data class OperatorRemediationRequest(
    val action: OperatorRemediationAction,
    val note: String? = null,
)

data class OperatorRemediationQueueItemResponse(
    val sourceType: String,
    val sourceKey: String,
    val queueStatus: String,
    val failureCategory: String,
    val resourceType: String,
    val resourceId: Long?,
    val detail: String,
    val retryable: Boolean,
    val availableActions: List<String>,
    val sourceOccurredAtUtc: String,
    val sourceUpdatedAtUtc: String,
    val actionRetryCount: Int,
    val lastQueueAction: String?,
    val lastActionByUserId: Long?,
    val lastActionAtUtc: String?,
    val lastActionNote: String?,
)

private fun com.demo.tourwave.application.operations.OperatorRemediationQueueItem.toResponse(): OperatorRemediationQueueItemResponse =
    OperatorRemediationQueueItemResponse(
        sourceType = sourceType.name,
        sourceKey = sourceKey,
        queueStatus = queueStatus.name,
        failureCategory = failureCategory,
        resourceType = resourceType,
        resourceId = resourceId,
        detail = detail,
        retryable = retryable,
        availableActions = availableActions.map { it.name }.sorted(),
        sourceOccurredAtUtc = sourceOccurredAtUtc.toString(),
        sourceUpdatedAtUtc = sourceUpdatedAtUtc.toString(),
        actionRetryCount = actionRetryCount,
        lastQueueAction = lastQueueAction?.name,
        lastActionByUserId = lastActionByUserId,
        lastActionAtUtc = lastActionAtUtc?.toString(),
        lastActionNote = lastActionNote,
    )
