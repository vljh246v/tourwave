package com.demo.tourwave.adapter.out.persistence.asset

import com.demo.tourwave.application.asset.port.AssetStoragePort
import com.demo.tourwave.application.asset.port.AssetUploadDescriptor
import com.demo.tourwave.application.asset.port.AssetUploadVerificationRequest
import com.demo.tourwave.application.asset.port.StoredAssetMetadata
import com.demo.tourwave.domain.common.DomainException
import com.demo.tourwave.domain.common.ErrorCode
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.time.Clock
import java.time.Instant
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@Component
@Profile("alpha", "beta", "real")
class HttpAssetStorageAdapter(
    @Value("\${integration.asset.base-url}") private val baseUrl: String,
    @Value("\${integration.asset.public-base-url}") private val publicBaseUrl: String,
    @Value("\${integration.asset.bucket}") private val bucket: String,
    @Value("\${integration.asset.access-key}") private val accessKey: String,
    @Value("\${integration.asset.secret-key}") private val secretKey: String,
    @Value("\${integration.asset.presign-ttl-seconds:900}") private val presignTtlSeconds: Long,
    private val clock: Clock
) : AssetStoragePort {
    private val httpClient = HttpClient.newBuilder().build()

    override fun issueUpload(ownerUserId: Long, assetIdHint: Long?, fileName: String, contentType: String): AssetUploadDescriptor {
        val assetToken = assetIdHint?.toString() ?: UUID.randomUUID().toString()
        val encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8)
        val storageKey = "users/$ownerUserId/assets/$assetToken/$encodedFileName"
        val expiresAtEpochSeconds = clock.instant().plusSeconds(presignTtlSeconds).epochSecond
        val signature = sign("PUT\n$bucket\n$storageKey\n$contentType\n$expiresAtEpochSeconds")
        val normalizedBaseUrl = baseUrl.trimEnd('/')
        val normalizedPublicBaseUrl = publicBaseUrl.trimEnd('/')
        return AssetUploadDescriptor(
            storageKey = storageKey,
            uploadUrl = "$normalizedBaseUrl/$bucket/$storageKey?expires=$expiresAtEpochSeconds&accessKey=$accessKey&signature=$signature",
            publicUrl = "$normalizedPublicBaseUrl/$bucket/$storageKey"
        )
    }

    override fun verifyUpload(request: AssetUploadVerificationRequest): StoredAssetMetadata {
        val timestamp = clock.instant().epochSecond
        val signature = sign("HEAD\n$bucket\n${request.storageKey}\n$timestamp")
        val target = URI.create("${baseUrl.trimEnd('/')}/$bucket/${request.storageKey}")
        val response = httpClient.send(
            HttpRequest.newBuilder(target)
                .header("X-Storage-Access-Key", accessKey)
                .header("X-Storage-Signature", signature)
                .header("X-Storage-Timestamp", timestamp.toString())
                .method("HEAD", HttpRequest.BodyPublishers.noBody())
                .build(),
            HttpResponse.BodyHandlers.discarding()
        )
        if (response.statusCode() == 404) {
            throw validation("uploaded asset object does not exist")
        }
        if (response.statusCode() >= 500 || response.statusCode() == 429) {
            throw DomainException(
                errorCode = ErrorCode.VALIDATION_ERROR,
                status = 503,
                message = "asset storage verification is temporarily unavailable"
            )
        }
        if (response.statusCode() !in 200..299) {
            throw validation("asset storage rejected upload verification")
        }
        val contentType = response.headers().firstValue("Content-Type").orElse("").trim().lowercase()
        if (contentType.isBlank()) {
            throw validation("uploaded asset metadata is missing content type")
        }
        val sizeBytes = response.headers().firstValue("Content-Length").orElse("").toLongOrNull()
            ?: throw validation("uploaded asset metadata is missing content length")
        val checksum = response.headers().firstValue("X-Checksum-Sha256").orElse(null)?.trim()?.lowercase()?.ifBlank { null }
        return StoredAssetMetadata(
            publicUrl = "${publicBaseUrl.trimEnd('/')}/$bucket/${request.storageKey}",
            contentType = contentType,
            sizeBytes = sizeBytes,
            checksumSha256 = checksum
        )
    }

    private fun sign(payload: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secretKey.toByteArray(StandardCharsets.UTF_8), "HmacSHA256"))
        return mac.doFinal(payload.toByteArray(StandardCharsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }

    private fun validation(message: String) = DomainException(
        errorCode = ErrorCode.VALIDATION_ERROR,
        status = 422,
        message = message
    )
}
