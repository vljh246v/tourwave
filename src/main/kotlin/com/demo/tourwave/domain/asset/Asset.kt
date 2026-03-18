package com.demo.tourwave.domain.asset

import java.time.Instant

enum class AssetStatus {
    UPLOADING,
    READY
}

data class Asset(
    val id: Long? = null,
    val ownerUserId: Long,
    val organizationId: Long? = null,
    val status: AssetStatus = AssetStatus.UPLOADING,
    val fileName: String,
    val contentType: String,
    val storageKey: String,
    val uploadUrl: String,
    val publicUrl: String? = null,
    val sizeBytes: Long? = null,
    val checksumSha256: String? = null,
    val createdAt: Instant,
    val completedAt: Instant? = null
) {
    fun complete(
        publicUrl: String,
        sizeBytes: Long?,
        checksumSha256: String?,
        now: Instant
    ): Asset {
        require(publicUrl.isNotBlank()) { "publicUrl must not be blank" }
        require(sizeBytes == null || sizeBytes >= 0) { "sizeBytes must be >= 0" }
        return copy(
            status = AssetStatus.READY,
            publicUrl = publicUrl,
            sizeBytes = sizeBytes,
            checksumSha256 = checksumSha256,
            completedAt = now
        )
    }

    companion object {
        fun create(
            ownerUserId: Long,
            organizationId: Long?,
            fileName: String,
            contentType: String,
            storageKey: String,
            uploadUrl: String,
            now: Instant
        ): Asset {
            return Asset(
                ownerUserId = ownerUserId,
                organizationId = organizationId,
                fileName = fileName,
                contentType = contentType,
                storageKey = storageKey,
                uploadUrl = uploadUrl,
                createdAt = now
            )
        }
    }
}
