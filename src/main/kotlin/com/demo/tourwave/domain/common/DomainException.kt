package com.demo.tourwave.domain.common

class DomainException(
    val errorCode: ErrorCode,
    val status: Int,
    override val message: String,
    val details: Map<String, Any?>? = null
) : RuntimeException(message)
