package com.demo.tourwave.application.tour

import com.demo.tourwave.application.organization.normalizeOptionalText
import com.demo.tourwave.domain.common.DomainException
import com.demo.tourwave.domain.common.ErrorCode

internal fun requireValidTourTitle(title: String): String {
    val normalized = title.trim()
    if (normalized.isEmpty()) {
        throw DomainException(
            errorCode = ErrorCode.REQUIRED_FIELD_MISSING,
            status = 422,
            message = "title is required",
        )
    }
    if (normalized.length > 255) {
        throw DomainException(
            errorCode = ErrorCode.VALIDATION_ERROR,
            status = 422,
            message = "title exceeds max length",
        )
    }
    return normalized
}

internal fun normalizeOptionalTourSummary(summary: String?): String? = normalizeOptionalText(summary, 2000, "summary")

internal fun normalizeOptionalTourDescription(description: String?): String? = normalizeOptionalText(description, 12000, "description")

internal fun normalizeStringList(
    values: List<String>,
    fieldName: String,
    maxItems: Int = 20,
    maxLength: Int = 80,
): List<String> {
    if (values.size > maxItems) {
        throw DomainException(
            errorCode = ErrorCode.VALIDATION_ERROR,
            status = 422,
            message = "$fieldName exceeds maximum item count",
        )
    }
    return values.map { value ->
        val normalized = value.trim()
        if (normalized.isEmpty()) {
            throw DomainException(
                errorCode = ErrorCode.VALIDATION_ERROR,
                status = 422,
                message = "$fieldName cannot contain blank items",
            )
        }
        if (normalized.length > maxLength) {
            throw DomainException(
                errorCode = ErrorCode.VALIDATION_ERROR,
                status = 422,
                message = "$fieldName item exceeds max length",
            )
        }
        normalized
    }.distinct()
}
