package com.demo.tourwave.adapter.out.persistence.asset

import com.demo.tourwave.application.asset.port.AssetRepository
import com.demo.tourwave.domain.asset.Asset
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Repository
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

@Repository
@Profile("!mysql & !mysql-test")
class InMemoryAssetRepositoryAdapter : AssetRepository {
    private val sequence = AtomicLong(0)
    private val assets = ConcurrentHashMap<Long, Asset>()

    override fun save(asset: Asset): Asset {
        val assetId = asset.id ?: sequence.incrementAndGet()
        val saved = asset.copy(id = assetId)
        assets[assetId] = saved
        sequence.updateAndGet { maxOf(it, assetId) }
        return saved
    }

    override fun findById(assetId: Long): Asset? = assets[assetId]

    override fun findAllByIds(assetIds: List<Long>): List<Asset> = assetIds.mapNotNull(assets::get)

    override fun clear() {
        assets.clear()
        sequence.set(0)
    }
}
