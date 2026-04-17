package com.demo.tourwave.application.auth

import com.demo.tourwave.domain.common.DomainException
import com.demo.tourwave.domain.common.ErrorCode

internal fun requireValidEmail(email: String): String {
    val normalized = email.trim().lowercase()
    if (normalized.isBlank() || !normalized.contains("@")) {
        throw DomainException(
            errorCode = ErrorCode.VALIDATION_ERROR,
            status = 422,
            message = "email must be a valid address",
        )
    }
    return normalized
}

internal fun requireValidPassword(password: String): String {
    val normalized = password.trim()
    if (normalized.length < 8) {
        throw DomainException(
            errorCode = ErrorCode.VALIDATION_ERROR,
            status = 422,
            message = "password must be at least 8 characters",
        )
    }
    return password
}

internal fun requireValidDisplayName(displayName: String): String {
    val normalized = displayName.trim()
    if (normalized.isBlank() || normalized.length > 100) {
        throw DomainException(
            errorCode = ErrorCode.VALIDATION_ERROR,
            status = 422,
            message = "displayName must be between 1 and 100 characters",
        )
    }
    return normalized
}
