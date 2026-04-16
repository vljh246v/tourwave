package com.demo.tourwave.domain.tour

data class TourContent(
    val description: String? = null,
    val highlights: List<String> = emptyList(),
    val inclusions: List<String> = emptyList(),
    val exclusions: List<String> = emptyList(),
    val preparations: List<String> = emptyList(),
    val policies: List<String> = emptyList(),
)
