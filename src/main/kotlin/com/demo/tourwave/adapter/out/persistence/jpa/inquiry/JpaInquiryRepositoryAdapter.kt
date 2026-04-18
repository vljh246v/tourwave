package com.demo.tourwave.adapter.out.persistence.jpa.inquiry

import com.demo.tourwave.application.inquiry.port.InquiryRepository
import com.demo.tourwave.domain.inquiry.Inquiry
import com.demo.tourwave.domain.inquiry.InquiryMessage
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Repository

@Repository
@Profile("mysql", "mysql-test")
class JpaInquiryRepositoryAdapter(
    private val inquiryJpaRepository: InquiryJpaRepository,
    private val inquiryMessageJpaRepository: InquiryMessageJpaRepository,
) : InquiryRepository {
    override fun save(inquiry: Inquiry): Inquiry = inquiryJpaRepository.save(inquiry.toEntity()).toDomain()

    override fun findById(inquiryId: Long): Inquiry? = inquiryJpaRepository.findById(inquiryId).orElse(null)?.toDomain()

    override fun findByBookingId(bookingId: Long): Inquiry? = inquiryJpaRepository.findByBookingId(bookingId)?.toDomain()

    override fun findByCreatedByUserId(createdByUserId: Long): List<Inquiry> =
        inquiryJpaRepository.findByCreatedByUserIdOrderByCreatedAtDescIdDesc(createdByUserId).map { it.toDomain() }

    override fun saveMessage(message: InquiryMessage): InquiryMessage = inquiryMessageJpaRepository.save(message.toEntity()).toDomain()

    override fun findMessageById(messageId: Long): InquiryMessage? = inquiryMessageJpaRepository.findById(messageId).orElse(null)?.toDomain()

    override fun findMessagesByInquiryId(inquiryId: Long): List<InquiryMessage> =
        inquiryMessageJpaRepository.findByInquiryIdOrderByIdAsc(inquiryId).map { it.toDomain() }

    override fun clear() {
        inquiryMessageJpaRepository.deleteAllInBatch()
        inquiryJpaRepository.deleteAllInBatch()
    }
}

private fun Inquiry.toEntity(): InquiryJpaEntity =
    InquiryJpaEntity(
        id = id,
        organizationId = organizationId,
        occurrenceId = occurrenceId,
        bookingId = bookingId,
        createdByUserId = createdByUserId,
        subject = subject,
        status = status,
        createdAt = createdAt,
    )

private fun InquiryJpaEntity.toDomain(): Inquiry =
    Inquiry(
        id = id,
        organizationId = organizationId,
        occurrenceId = occurrenceId,
        bookingId = bookingId,
        createdByUserId = createdByUserId,
        subject = subject,
        status = status,
        createdAt = createdAt,
    )

private fun InquiryMessage.toEntity(): InquiryMessageJpaEntity =
    InquiryMessageJpaEntity(
        id = id,
        inquiryId = inquiryId,
        senderUserId = senderUserId,
        body = body,
        attachmentAssetIdsCsv = attachmentAssetIds?.joinToString(","),
        createdAt = createdAt,
    )

private fun InquiryMessageJpaEntity.toDomain(): InquiryMessage =
    InquiryMessage(
        id = id,
        inquiryId = inquiryId,
        senderUserId = senderUserId,
        body = body,
        attachmentAssetIds =
            attachmentAssetIdsCsv
                ?.takeIf { it.isNotBlank() }
                ?.split(",")
                ?.map { it.toLong() },
        createdAt = createdAt,
    )
