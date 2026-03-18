package com.demo.tourwave.adapter.out.persistence.asset

import com.demo.tourwave.application.asset.port.AssetStoragePort
import com.demo.tourwave.application.asset.port.AssetUploadDescriptor
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.UUID

@Component
class FakeAssetStorageAdapter(
    @Value("\${integration.asset.base-url:http://localhost:18082/mock-asset}") private val baseUrl: String
) : AssetStoragePort {
    override fun issueUpload(ownerUserId: Long, assetIdHint: Long?, fileName: String, contentType: String): AssetUploadDescriptor {
        val assetToken = assetIdHint?.toString() ?: UUID.randomUUID().toString()
        val encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8)
        val storageKey = "users/$ownerUserId/assets/$assetToken/$encodedFileName"
        val normalizedBase = baseUrl.trimEnd('/')
        return AssetUploadDescriptor(
            storageKey = storageKey,
            uploadUrl = "$normalizedBase/uploads/$storageKey",
            publicUrl = "$normalizedBase/public/$storageKey"
        )
    }
}
