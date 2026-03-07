package com.demo.tourwave.adapter.`in`.web.error

import com.demo.tourwave.domain.common.DomainException
import com.demo.tourwave.domain.common.ErrorCode
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingRequestHeaderException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(DomainException::class)
    fun handleDomainException(e: DomainException): ResponseEntity<ErrorResponse> {
        val body = ErrorResponse(
            error = ErrorResponse.ErrorDetail(
                code = e.errorCode,
                message = e.message,
                details = e.details
            )
        )
        return ResponseEntity.status(e.status).body(body)
    }

    @ExceptionHandler(
        MethodArgumentNotValidException::class,
        HttpMessageNotReadableException::class,
        MissingRequestHeaderException::class,
        MethodArgumentTypeMismatchException::class,
        IllegalArgumentException::class
    )
    fun handleValidation(e: Exception): ResponseEntity<ErrorResponse> {
        val body = ErrorResponse(
            error = ErrorResponse.ErrorDetail(
                code = ErrorCode.VALIDATION_ERROR,
                message = "Invalid request"
            )
        )
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(body)
    }
}
