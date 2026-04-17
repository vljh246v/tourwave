package com.demo.tourwave.domain.tour

import java.time.Instant

data class Tour(
    val id: Long? = null,
    val organizationId: Long,
    val title: String,
    val summary: String? = null,
    val status: TourStatus = TourStatus.DRAFT,
    val content: TourContent = TourContent(),
    val attachmentAssetIds: List<Long> = emptyList(),
    val publishedAt: Instant? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    fun updateMetadata(
        title: String,
        summary: String?,
        now: Instant,
    ): Tour {
        return copy(
            title = title,
            summary = summary,
            updatedAt = now,
        )
    }

    fun updateContent(
        content: TourContent,
        now: Instant,
    ): Tour {
        return copy(
            content = content,
            updatedAt = now,
        )
    }

    fun updateAttachments(
        assetIds: List<Long>,
        now: Instant,
    ): Tour {
        return copy(
            attachmentAssetIds = assetIds,
            updatedAt = now,
        )
    }

    fun publish(now: Instant): Tour {
        return copy(
            status = TourStatus.PUBLISHED,
            publishedAt = publishedAt ?: now,
            updatedAt = now,
        )
    }

    companion object {
        fun create(
            organizationId: Long,
            title: String,
            summary: String?,
            now: Instant,
        ): Tour {
            return Tour(
                organizationId = organizationId,
                title = title,
                summary = summary,
                status = TourStatus.DRAFT,
                createdAt = now,
                updatedAt = now,
            )
        }
    }
}
