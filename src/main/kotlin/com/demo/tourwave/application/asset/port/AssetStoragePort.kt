package com.demo.tourwave.application.asset.port

data class AssetUploadDescriptor(
    val storageKey: String,
    val uploadUrl: String,
    val publicUrl: String
)

interface AssetStoragePort {
    fun issueUpload(
        ownerUserId: Long,
        assetIdHint: Long?,
        fileName: String,
        contentType: String
    ): AssetUploadDescriptor
}
