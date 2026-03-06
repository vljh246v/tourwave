package com.demo.tourwave.adapter.`in`.web.error

import com.demo.tourwave.domain.common.ErrorCode

data class ErrorResponse(
    val error: ErrorDetail
) {
    data class ErrorDetail(
        val code: ErrorCode,
        val message: String,
        val details: Map<String, Any?>? = null
    )
}
