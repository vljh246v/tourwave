package com.demo.tourwave.adapter.out.persistence.jpa.asset

import com.demo.tourwave.application.asset.port.AssetRepository
import com.demo.tourwave.domain.asset.Asset
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Repository

@Repository
@Profile("mysql", "mysql-test")
class JpaAssetRepositoryAdapter(
    private val assetJpaRepository: AssetJpaRepository
) : AssetRepository {
    override fun save(asset: Asset): Asset = assetJpaRepository.save(asset.toEntity()).toDomain()

    override fun findById(assetId: Long): Asset? = assetJpaRepository.findById(assetId).orElse(null)?.toDomain()

    override fun findAllByIds(assetIds: List<Long>): List<Asset> = assetJpaRepository.findAllById(assetIds).map { it.toDomain() }

    override fun clear() {
        assetJpaRepository.deleteAllInBatch()
    }
}

private fun Asset.toEntity(): AssetJpaEntity =
    AssetJpaEntity(
        id = id,
        ownerUserId = ownerUserId,
        organizationId = organizationId,
        status = status,
        fileName = fileName,
        contentType = contentType,
        storageKey = storageKey,
        uploadUrl = uploadUrl,
        publicUrl = publicUrl,
        sizeBytes = sizeBytes,
        checksumSha256 = checksumSha256,
        createdAt = createdAt,
        completedAt = completedAt
    )

private fun AssetJpaEntity.toDomain(): Asset =
    Asset(
        id = id,
        ownerUserId = ownerUserId,
        organizationId = organizationId,
        status = status,
        fileName = fileName,
        contentType = contentType,
        storageKey = storageKey,
        uploadUrl = uploadUrl,
        publicUrl = publicUrl,
        sizeBytes = sizeBytes,
        checksumSha256 = checksumSha256,
        createdAt = createdAt,
        completedAt = completedAt
    )
