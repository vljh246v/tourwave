package com.demo.tourwave.application.topology

import com.demo.tourwave.application.organization.normalizeOptionalText
import com.demo.tourwave.domain.common.DomainException
import com.demo.tourwave.domain.common.ErrorCode

internal fun normalizeOptionalHeadline(value: String?): String? =
    normalizeOptionalText(value, 255, "headline")

internal fun normalizeOptionalInstructorBio(value: String?): String? =
    normalizeOptionalText(value, 4000, "bio")

internal fun normalizeOptionalInternalNote(value: String?): String? =
    normalizeOptionalText(value, 4000, "internalNote")

internal fun normalizeStringList(values: List<String>, fieldName: String, maxItems: Int = 20, maxLength: Int = 80): List<String> {
    if (values.size > maxItems) {
        throw DomainException(
            errorCode = ErrorCode.VALIDATION_ERROR,
            status = 422,
            message = "$fieldName exceeds maximum item count"
        )
    }
    return values.map { value ->
        val normalized = value.trim()
        if (normalized.isEmpty()) {
            throw DomainException(
                errorCode = ErrorCode.VALIDATION_ERROR,
                status = 422,
                message = "$fieldName cannot contain blank items"
            )
        }
        if (normalized.length > maxLength) {
            throw DomainException(
                errorCode = ErrorCode.VALIDATION_ERROR,
                status = 422,
                message = "$fieldName item exceeds max length"
            )
        }
        normalized
    }.distinct()
}

internal fun normalizeYearsOfExperience(value: Int?): Int? {
    if (value == null) {
        return null
    }
    if (value !in 0..60) {
        throw DomainException(
            errorCode = ErrorCode.VALIDATION_ERROR,
            status = 422,
            message = "yearsOfExperience must be between 0 and 60"
        )
    }
    return value
}
