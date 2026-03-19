package com.demo.tourwave.adapter.out.persistence.jpa.operations

import com.demo.tourwave.domain.operations.OperatorFailureSourceType
import org.springframework.data.jpa.repository.JpaRepository

interface OperatorFailureRecordJpaRepository : JpaRepository<OperatorFailureRecordJpaEntity, Long> {
    fun findBySourceTypeAndSourceKey(sourceType: OperatorFailureSourceType, sourceKey: String): OperatorFailureRecordJpaEntity?
    fun findAllByOrderByUpdatedAtUtcDesc(): List<OperatorFailureRecordJpaEntity>
}
