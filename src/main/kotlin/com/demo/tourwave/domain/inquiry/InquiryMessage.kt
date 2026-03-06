package com.demo.tourwave.domain.inquiry

import java.time.Instant

data class InquiryMessage(
    val id: Long? = null,
    val inquiryId: Long,
    val senderUserId: Long,
    val body: String,
    val attachmentAssetIds: List<Long>? = null,
    val createdAt: Instant = Instant.now()
)

