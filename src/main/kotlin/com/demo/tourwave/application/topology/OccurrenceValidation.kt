package com.demo.tourwave.application.topology

import com.demo.tourwave.domain.common.DomainException
import com.demo.tourwave.domain.common.ErrorCode
import java.time.Instant

internal fun requireValidOccurrenceCapacity(capacity: Int): Int {
    if (capacity !in 1..500) {
        throw DomainException(
            errorCode = ErrorCode.VALIDATION_ERROR,
            status = 422,
            message = "capacity must be between 1 and 500"
        )
    }
    return capacity
}

internal fun requireValidOccurrenceWindow(
    startsAtUtc: Instant,
    endsAtUtc: Instant
) {
    if (!endsAtUtc.isAfter(startsAtUtc)) {
        throw DomainException(
            errorCode = ErrorCode.VALIDATION_ERROR,
            status = 422,
            message = "end time must be after start time"
        )
    }
}

internal fun requireValidCurrency(currency: String): String {
    val normalized = currency.trim().uppercase()
    if (!normalized.matches(Regex("[A-Z]{3}"))) {
        throw DomainException(
            errorCode = ErrorCode.VALIDATION_ERROR,
            status = 422,
            message = "currency must be an ISO-4217 style 3-letter code"
        )
    }
    return normalized
}

internal fun requireValidUnitPrice(unitPrice: Int): Int {
    if (unitPrice < 0) {
        throw DomainException(
            errorCode = ErrorCode.VALIDATION_ERROR,
            status = 422,
            message = "unit price cannot be negative"
        )
    }
    return unitPrice
}

internal fun normalizeOptionalShortText(
    value: String?,
    fieldName: String,
    maxLength: Int
): String? = normalizeOptionalText(value, maxLength, fieldName)

internal fun requireValidPartySize(partySize: Int): Int {
    if (partySize < 1) {
        throw DomainException(
            errorCode = ErrorCode.PARTY_SIZE_OUT_OF_RANGE,
            status = 422,
            message = "party size must be at least 1"
        )
    }
    return partySize
}

internal fun requireValidLimit(limit: Int?): Int {
    val resolved = limit ?: 20
    if (resolved !in 1..100) {
        throw DomainException(
            errorCode = ErrorCode.VALIDATION_ERROR,
            status = 422,
            message = "limit must be between 1 and 100"
        )
    }
    return resolved
}

internal fun parseCursor(cursor: String?): Long? {
    if (cursor.isNullOrBlank()) {
        return null
    }
    return cursor.toLongOrNull() ?: throw DomainException(
        errorCode = ErrorCode.VALIDATION_ERROR,
        status = 422,
        message = "cursor must be a numeric occurrence id"
    )
}
