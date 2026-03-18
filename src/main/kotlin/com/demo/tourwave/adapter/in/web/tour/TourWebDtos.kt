package com.demo.tourwave.adapter.`in`.web.tour

import com.demo.tourwave.domain.tour.Tour
import com.demo.tourwave.domain.tour.TourContent
import java.time.Instant

data class CreateTourWebRequest(
    val title: String,
    val summary: String? = null
)

data class UpdateTourWebRequest(
    val title: String,
    val summary: String? = null
)

data class UpdateTourContentWebRequest(
    val description: String? = null,
    val highlights: List<String> = emptyList(),
    val inclusions: List<String> = emptyList(),
    val exclusions: List<String> = emptyList(),
    val preparations: List<String> = emptyList(),
    val policies: List<String> = emptyList()
)

data class TourResponse(
    val id: Long,
    val organizationId: Long,
    val title: String,
    val summary: String?,
    val attachmentAssetIds: List<Long>,
    val status: String,
    val publishedAt: Instant?
)

data class TourContentResponse(
    val description: String?,
    val highlights: List<String>,
    val inclusions: List<String>,
    val exclusions: List<String>,
    val preparations: List<String>,
    val policies: List<String>
)

fun Tour.toResponse(): TourResponse =
    TourResponse(
        id = requireNotNull(id),
        organizationId = organizationId,
        title = title,
        summary = summary,
        attachmentAssetIds = attachmentAssetIds,
        status = status.name,
        publishedAt = publishedAt
    )

fun TourContent.toResponse(): TourContentResponse =
    TourContentResponse(
        description = description,
        highlights = highlights,
        inclusions = inclusions,
        exclusions = exclusions,
        preparations = preparations,
        policies = policies
    )

data class PublicTourResponse(
    val id: Long,
    val organizationId: Long,
    val title: String,
    val summary: String?,
    val description: String?,
    val highlights: List<String>,
    val attachmentAssetIds: List<Long>,
    val publishedAt: Instant?
)

data class TourCatalogListResponse(
    val items: List<PublicTourResponse>,
    val nextCursor: String?
)
