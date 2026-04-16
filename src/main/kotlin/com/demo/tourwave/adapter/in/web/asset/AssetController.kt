package com.demo.tourwave.adapter.`in`.web.asset

import com.demo.tourwave.application.asset.AssetCommandService
import com.demo.tourwave.application.asset.AttachOrganizationAssetsCommand
import com.demo.tourwave.application.asset.AttachTourAssetsCommand
import com.demo.tourwave.application.asset.CompleteAssetUploadCommand
import com.demo.tourwave.application.asset.IssueAssetUploadCommand
import com.demo.tourwave.application.common.port.AuthzGuardPort
import com.demo.tourwave.domain.asset.Asset
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
class AssetController(
    private val assetCommandService: AssetCommandService,
    private val authzGuardPort: AuthzGuardPort,
) {
    @PostMapping("/assets/uploads")
    fun issueUpload(
        @RequestHeader("X-Actor-User-Id", required = false) actorUserId: Long?,
        @RequestBody request: IssueAssetUploadRequest,
    ): ResponseEntity<AssetResponse> {
        val requiredActorUserId = authzGuardPort.requireActorUserId(actorUserId)
        val asset =
            assetCommandService.issueUpload(
                IssueAssetUploadCommand(
                    actorUserId = requiredActorUserId,
                    organizationId = request.organizationId,
                    fileName = request.fileName,
                    contentType = request.contentType,
                ),
            )
        return ResponseEntity.status(201).body(asset.toResponse())
    }

    @PostMapping("/assets/{assetId}/complete")
    fun completeUpload(
        @PathVariable assetId: Long,
        @RequestHeader("X-Actor-User-Id", required = false) actorUserId: Long?,
        @RequestBody request: CompleteAssetUploadRequest,
    ): ResponseEntity<AssetResponse> {
        val requiredActorUserId = authzGuardPort.requireActorUserId(actorUserId)
        return ResponseEntity.ok(
            assetCommandService.completeUpload(
                CompleteAssetUploadCommand(
                    actorUserId = requiredActorUserId,
                    assetId = assetId,
                    sizeBytes = request.sizeBytes,
                    checksumSha256 = request.checksumSha256,
                ),
            ).toResponse(),
        )
    }

    @PutMapping("/operator/organizations/{organizationId}/assets")
    fun attachOrganizationAssets(
        @PathVariable organizationId: Long,
        @RequestHeader("X-Actor-User-Id", required = false) actorUserId: Long?,
        @RequestBody request: AssetAttachmentRequest,
    ): ResponseEntity<AssetAttachmentResponse> {
        val requiredActorUserId = authzGuardPort.requireActorUserId(actorUserId)
        return ResponseEntity.ok(
            AssetAttachmentResponse(
                assetIds =
                    assetCommandService.attachOrganizationAssets(
                        AttachOrganizationAssetsCommand(
                            actorUserId = requiredActorUserId,
                            organizationId = organizationId,
                            assetIds = request.assetIds,
                        ),
                    ),
            ),
        )
    }

    @PutMapping("/tours/{tourId}/assets")
    fun attachTourAssets(
        @PathVariable tourId: Long,
        @RequestHeader("X-Actor-User-Id", required = false) actorUserId: Long?,
        @RequestBody request: AssetAttachmentRequest,
    ): ResponseEntity<AssetAttachmentResponse> {
        val requiredActorUserId = authzGuardPort.requireActorUserId(actorUserId)
        return ResponseEntity.ok(
            AssetAttachmentResponse(
                assetIds =
                    assetCommandService.attachTourAssets(
                        AttachTourAssetsCommand(
                            actorUserId = requiredActorUserId,
                            tourId = tourId,
                            assetIds = request.assetIds,
                        ),
                    ),
            ),
        )
    }
}

data class IssueAssetUploadRequest(
    val organizationId: Long? = null,
    val fileName: String,
    val contentType: String,
)

data class CompleteAssetUploadRequest(
    val sizeBytes: Long? = null,
    val checksumSha256: String? = null,
)

data class AssetAttachmentRequest(
    val assetIds: List<Long> = emptyList(),
)

data class AssetAttachmentResponse(
    val assetIds: List<Long>,
)

data class AssetResponse(
    val id: Long,
    val ownerUserId: Long,
    val organizationId: Long?,
    val status: String,
    val fileName: String,
    val contentType: String,
    val uploadUrl: String,
    val publicUrl: String?,
    val sizeBytes: Long?,
    val createdAt: Instant,
    val completedAt: Instant?,
)

private fun Asset.toResponse(): AssetResponse =
    AssetResponse(
        id = requireNotNull(id),
        ownerUserId = ownerUserId,
        organizationId = organizationId,
        status = status.name,
        fileName = fileName,
        contentType = contentType,
        uploadUrl = uploadUrl,
        publicUrl = publicUrl,
        sizeBytes = sizeBytes,
        createdAt = createdAt,
        completedAt = completedAt,
    )
