package com.demo.tourwave.application.organization

import com.demo.tourwave.application.auth.requireValidEmail
import com.demo.tourwave.domain.common.DomainException
import com.demo.tourwave.domain.common.ErrorCode

fun requireValidOrganizationSlug(slug: String): String {
    val normalized = slug.trim().lowercase()
    if (!normalized.matches(Regex("^[a-z0-9]+(?:-[a-z0-9]+)*$"))) {
        throw DomainException(
            errorCode = ErrorCode.VALIDATION_ERROR,
            status = 422,
            message = "organization slug is invalid"
        )
    }
    return normalized
}

fun requireValidOrganizationName(name: String): String {
    val normalized = name.trim()
    if (normalized.length !in 2..120) {
        throw DomainException(
            errorCode = ErrorCode.VALIDATION_ERROR,
            status = 422,
            message = "organization name must be between 2 and 120 characters"
        )
    }
    return normalized
}

fun normalizeOptionalText(value: String?, maxLength: Int, fieldName: String): String? {
    val normalized = value?.trim()?.ifEmpty { null } ?: return null
    if (normalized.length > maxLength) {
        throw DomainException(
            errorCode = ErrorCode.VALIDATION_ERROR,
            status = 422,
            message = "$fieldName is too long"
        )
    }
    return normalized
}

fun normalizeOptionalEmail(value: String?): String? = value?.let(::requireValidEmail)

fun normalizeOptionalPhone(value: String?): String? {
    val normalized = value?.trim()?.ifEmpty { null } ?: return null
    if (!normalized.matches(Regex("^[0-9+()\\-\\s]{7,32}$"))) {
        throw DomainException(
            errorCode = ErrorCode.VALIDATION_ERROR,
            status = 422,
            message = "contact phone is invalid"
        )
    }
    return normalized
}

fun normalizeOptionalUrl(value: String?): String? {
    val normalized = value?.trim()?.ifEmpty { null } ?: return null
    if (!normalized.startsWith("http://") && !normalized.startsWith("https://")) {
        throw DomainException(
            errorCode = ErrorCode.VALIDATION_ERROR,
            status = 422,
            message = "website url must start with http:// or https://"
        )
    }
    if (normalized.length > 512) {
        throw DomainException(
            errorCode = ErrorCode.VALIDATION_ERROR,
            status = 422,
            message = "website url is too long"
        )
    }
    return normalized
}

fun requireValidTimezone(timezone: String): String {
    val normalized = timezone.trim()
    return try {
        java.time.ZoneId.of(normalized)
        normalized
    } catch (_: Exception) {
        throw DomainException(
            errorCode = ErrorCode.VALIDATION_ERROR,
            status = 422,
            message = "timezone is invalid"
        )
    }
}
