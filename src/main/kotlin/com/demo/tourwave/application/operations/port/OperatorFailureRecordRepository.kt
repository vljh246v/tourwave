package com.demo.tourwave.application.operations.port

import com.demo.tourwave.domain.operations.OperatorFailureRecord
import com.demo.tourwave.domain.operations.OperatorFailureSourceType

interface OperatorFailureRecordRepository {
    fun save(record: OperatorFailureRecord): OperatorFailureRecord
    fun findBySource(sourceType: OperatorFailureSourceType, sourceKey: String): OperatorFailureRecord?
    fun findAll(): List<OperatorFailureRecord>
    fun clear()
}
