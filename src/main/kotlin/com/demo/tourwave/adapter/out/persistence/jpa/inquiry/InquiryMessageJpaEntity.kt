package com.demo.tourwave.adapter.out.persistence.jpa.inquiry

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Lob
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(
    name = "inquiry_messages",
    indexes = [Index(name = "idx_inquiry_messages_inquiry", columnList = "inquiry_id,created_at")]
)
data class InquiryMessageJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @Column(name = "inquiry_id", nullable = false)
    val inquiryId: Long,
    @Column(name = "sender_user_id", nullable = false)
    val senderUserId: Long,
    @Lob
    @Column(nullable = false)
    val body: String,
    @Column(name = "attachment_asset_ids_csv")
    val attachmentAssetIdsCsv: String? = null,
    @Column(name = "created_at", nullable = false)
    val createdAt: Instant
)
