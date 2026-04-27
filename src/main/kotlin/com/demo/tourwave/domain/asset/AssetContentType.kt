package com.demo.tourwave.domain.asset

import com.demo.tourwave.domain.common.DomainException
import com.demo.tourwave.domain.common.ErrorCode

enum class AssetContentType(val mimeType: String) {
    IMAGE_JPEG("image/jpeg"),
    IMAGE_PNG("image/png"),
    IMAGE_WEBP("image/webp"),
    IMAGE_GIF("image/gif"),
    APPLICATION_PDF("application/pdf"),
    ;

    companion object {
        private val ALLOWED: Map<String, AssetContentType> =
            entries.associateBy { it.mimeType }

        fun fromString(raw: String): AssetContentType {
            val normalized = raw.trim().lowercase()
            return ALLOWED[normalized]
                ?: throw DomainException(
                    errorCode = ErrorCode.ASSET_UNSUPPORTED_CONTENT_TYPE,
                    status = 422,
                    message = "content type '$normalized' is not supported. Allowed: ${ALLOWED.keys.sorted().joinToString()}",
                )
        }
    }
}
