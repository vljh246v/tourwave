package com.demo.tourwave.adapter.out.persistence.jpa.inquiry

import org.springframework.data.jpa.repository.JpaRepository

interface InquiryMessageJpaRepository : JpaRepository<InquiryMessageJpaEntity, Long> {
    fun findByInquiryIdOrderByIdAsc(inquiryId: Long): List<InquiryMessageJpaEntity>
}
