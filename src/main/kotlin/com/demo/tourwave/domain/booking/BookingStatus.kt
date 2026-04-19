package com.demo.tourwave.domain.booking

enum class BookingStatus {
    REQUESTED,
    WAITLISTED,
    OFFERED,
    CONFIRMED,
    REJECTED,
    CANCELED,
    EXPIRED,
    COMPLETED,
    ;

    fun isTerminal(): Boolean = this in setOf(REJECTED, CANCELED, EXPIRED, COMPLETED)
}
