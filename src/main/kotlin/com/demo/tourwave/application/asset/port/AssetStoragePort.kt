package com.demo.tourwave.application.asset.port

data class AssetUploadDescriptor(
    val storageKey: String,
    val uploadUrl: String,
    val publicUrl: String
)

data class AssetUploadVerificationRequest(
    val storageKey: String,
    val expectedContentType: String,
    val reportedSizeBytes: Long?,
    val reportedChecksumSha256: String?
)

data class StoredAssetMetadata(
    val publicUrl: String,
    val contentType: String,
    val sizeBytes: Long,
    val checksumSha256: String? = null
)

interface AssetStoragePort {
    fun issueUpload(
        ownerUserId: Long,
        assetIdHint: Long?,
        fileName: String,
        contentType: String
    ): AssetUploadDescriptor

    fun verifyUpload(request: AssetUploadVerificationRequest): StoredAssetMetadata
}
