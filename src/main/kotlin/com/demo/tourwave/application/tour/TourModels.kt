package com.demo.tourwave.application.tour

data class CreateTourCommand(
    val actorUserId: Long,
    val organizationId: Long,
    val title: String,
    val summary: String? = null,
)

data class UpdateTourCommand(
    val actorUserId: Long,
    val tourId: Long,
    val title: String,
    val summary: String? = null,
)

data class UpdateTourContentCommand(
    val actorUserId: Long,
    val tourId: Long,
    val description: String? = null,
    val highlights: List<String> = emptyList(),
    val inclusions: List<String> = emptyList(),
    val exclusions: List<String> = emptyList(),
    val preparations: List<String> = emptyList(),
    val policies: List<String> = emptyList(),
)

data class PublishTourCommand(
    val actorUserId: Long,
    val tourId: Long,
)

data class ArchiveTourCommand(
    val actorUserId: Long,
    val tourId: Long,
)
