package com.demo.tourwave.adapter.out.persistence.jpa.inquiry

import org.springframework.data.jpa.repository.JpaRepository

interface InquiryJpaRepository : JpaRepository<InquiryJpaEntity, Long> {
    fun findByBookingId(bookingId: Long): InquiryJpaEntity?

    fun findByCreatedByUserIdOrderByCreatedAtDescIdDesc(createdByUserId: Long): List<InquiryJpaEntity>
}
