package com.demo.tourwave.domain.tour

data class Tour(
    val id: Long? = null,
    val organizationId: Long,
    val title: String
)
