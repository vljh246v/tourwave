package com.demo.tourwave.application.asset

import com.demo.tourwave.application.asset.port.AssetRepository
import com.demo.tourwave.application.asset.port.AssetStoragePort
import com.demo.tourwave.application.asset.port.AssetUploadVerificationRequest
import com.demo.tourwave.application.organization.OrganizationAccessGuard
import com.demo.tourwave.application.organization.port.OrganizationRepository
import com.demo.tourwave.application.topology.port.TourRepository
import com.demo.tourwave.application.user.port.UserRepository
import com.demo.tourwave.domain.asset.Asset
import com.demo.tourwave.domain.asset.AssetStatus
import com.demo.tourwave.domain.common.DomainException
import com.demo.tourwave.domain.common.ErrorCode
import java.time.Clock

data class IssueAssetUploadCommand(
    val actorUserId: Long,
    val organizationId: Long? = null,
    val fileName: String,
    val contentType: String
)

data class CompleteAssetUploadCommand(
    val actorUserId: Long,
    val assetId: Long,
    val sizeBytes: Long?,
    val checksumSha256: String?
)

data class AttachOrganizationAssetsCommand(
    val actorUserId: Long,
    val organizationId: Long,
    val assetIds: List<Long>
)

data class AttachTourAssetsCommand(
    val actorUserId: Long,
    val tourId: Long,
    val assetIds: List<Long>
)

class AssetCommandService(
    private val assetRepository: AssetRepository,
    private val assetStoragePort: AssetStoragePort,
    private val organizationRepository: OrganizationRepository,
    private val tourRepository: TourRepository,
    private val organizationAccessGuard: OrganizationAccessGuard,
    private val userRepository: UserRepository,
    private val clock: Clock
) {
    fun issueUpload(command: IssueAssetUploadCommand): Asset {
        requireActor(command.actorUserId)
        command.organizationId?.let { organizationAccessGuard.requireMembership(command.actorUserId, it) }
        val normalizedFileName = requireFileName(command.fileName)
        val normalizedContentType = requireContentType(command.contentType)
        val upload = assetStoragePort.issueUpload(
            ownerUserId = command.actorUserId,
            assetIdHint = null,
            fileName = normalizedFileName,
            contentType = normalizedContentType
        )
        return assetRepository.save(
            Asset.create(
                ownerUserId = command.actorUserId,
                organizationId = command.organizationId,
                fileName = normalizedFileName,
                contentType = normalizedContentType,
                storageKey = upload.storageKey,
                uploadUrl = upload.uploadUrl,
                now = clock.instant()
            )
        )
    }

    fun completeUpload(command: CompleteAssetUploadCommand): Asset {
        val asset = requireAsset(command.assetId)
        if (asset.ownerUserId != command.actorUserId) {
            throw forbidden("asset ${command.assetId} is not owned by actor")
        }
        val normalizedChecksum = normalizeChecksum(command.checksumSha256)
        val storedMetadata = assetStoragePort.verifyUpload(
            AssetUploadVerificationRequest(
                storageKey = asset.storageKey,
                expectedContentType = asset.contentType,
                reportedSizeBytes = command.sizeBytes,
                reportedChecksumSha256 = normalizedChecksum
            )
        )
        if (storedMetadata.contentType != asset.contentType) {
            throw validation("uploaded asset content type does not match requested content type")
        }
        if (command.sizeBytes != null && storedMetadata.sizeBytes != command.sizeBytes) {
            throw validation("uploaded asset size does not match completion payload")
        }
        if (normalizedChecksum != null && storedMetadata.checksumSha256 != null && storedMetadata.checksumSha256 != normalizedChecksum) {
            throw validation("uploaded asset checksum does not match completion payload")
        }
        return assetRepository.save(
            asset.complete(
                publicUrl = storedMetadata.publicUrl,
                sizeBytes = storedMetadata.sizeBytes,
                checksumSha256 = storedMetadata.checksumSha256 ?: normalizedChecksum,
                now = clock.instant()
            )
        )
    }

    fun attachOrganizationAssets(command: AttachOrganizationAssetsCommand): List<Long> {
        val organization = organizationRepository.findById(command.organizationId) ?: throw notFound("organization ${command.organizationId} not found")
        organizationAccessGuard.requireOperator(command.actorUserId, command.organizationId)
        val assets = resolveAttachableAssets(
            actorUserId = command.actorUserId,
            organizationId = command.organizationId,
            assetIds = command.assetIds
        )
        organizationRepository.save(
            organization.updateAttachments(assets.map { requireNotNull(it.id) }, clock.instant())
        )
        return assets.map { requireNotNull(it.id) }
    }

    fun attachTourAssets(command: AttachTourAssetsCommand): List<Long> {
        val tour = tourRepository.findById(command.tourId) ?: throw notFound("tour ${command.tourId} not found")
        organizationAccessGuard.requireOperator(command.actorUserId, tour.organizationId)
        val assets = resolveAttachableAssets(
            actorUserId = command.actorUserId,
            organizationId = tour.organizationId,
            assetIds = command.assetIds
        )
        tourRepository.save(
            tour.updateAttachments(assets.map { requireNotNull(it.id) }, clock.instant())
        )
        return assets.map { requireNotNull(it.id) }
    }

    private fun resolveAttachableAssets(actorUserId: Long, organizationId: Long, assetIds: List<Long>): List<Asset> {
        val normalizedIds = normalizeAssetIds(assetIds)
        if (normalizedIds.isEmpty()) return emptyList()
        val assets = assetRepository.findAllByIds(normalizedIds)
        if (assets.size != normalizedIds.size) {
            throw notFound("one or more assets not found")
        }
        assets.forEach { asset ->
            if (asset.status != AssetStatus.READY) {
                throw validation("asset ${asset.id} is not ready")
            }
            if (asset.ownerUserId != actorUserId && asset.organizationId != organizationId) {
                throw forbidden("asset ${asset.id} cannot be attached by actor")
            }
        }
        return normalizedIds.map { assetId -> assets.first { it.id == assetId } }
    }

    private fun requireActor(actorUserId: Long) {
        userRepository.findById(actorUserId) ?: throw DomainException(
            errorCode = ErrorCode.UNAUTHORIZED,
            status = 401,
            message = "authenticated user does not exist"
        )
    }

    private fun requireAsset(assetId: Long): Asset =
        assetRepository.findById(assetId) ?: throw notFound("asset $assetId not found")

    private fun requireFileName(fileName: String): String {
        val normalized = fileName.trim()
        if (normalized.isBlank() || normalized.length > 255) {
            throw validation("fileName must be between 1 and 255 characters")
        }
        return normalized
    }

    private fun requireContentType(contentType: String): String {
        val normalized = contentType.trim().lowercase()
        if (normalized.isBlank() || !normalized.contains("/")) {
            throw validation("contentType must be a valid media type")
        }
        return normalized
    }

    private fun normalizeChecksum(raw: String?): String? {
        val normalized = raw?.trim()?.lowercase()?.ifBlank { null } ?: return null
        if (!normalized.matches(Regex("[a-f0-9]{32,128}"))) {
            throw validation("checksumSha256 must be a hex digest")
        }
        return normalized
    }

    private fun normalizeAssetIds(assetIds: List<Long>): List<Long> {
        val normalized = assetIds.distinct()
        if (normalized.any { it <= 0 }) {
            throw validation("assetIds must be positive")
        }
        return normalized
    }

    private fun validation(message: String) = DomainException(
        errorCode = ErrorCode.VALIDATION_ERROR,
        status = 422,
        message = message
    )

    private fun forbidden(message: String) = DomainException(
        errorCode = ErrorCode.FORBIDDEN,
        status = 403,
        message = message
    )

    private fun notFound(message: String) = DomainException(
        errorCode = ErrorCode.VALIDATION_ERROR,
        status = 404,
        message = message
    )
}
