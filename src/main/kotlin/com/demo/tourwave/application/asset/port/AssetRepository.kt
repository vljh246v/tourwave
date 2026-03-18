package com.demo.tourwave.application.asset.port

import com.demo.tourwave.domain.asset.Asset

interface AssetRepository {
    fun save(asset: Asset): Asset
    fun findById(assetId: Long): Asset?
    fun findAllByIds(assetIds: List<Long>): List<Asset>
    fun clear()
}
