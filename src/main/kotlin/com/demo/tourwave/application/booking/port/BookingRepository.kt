package com.demo.tourwave.application.booking.port

import com.demo.tourwave.domain.booking.Booking
import com.demo.tourwave.domain.booking.BookingStatus

interface BookingRepository {
    fun save(booking: Booking): Booking
    fun findById(bookingId: Long): Booking?
    fun findByOccurrenceAndStatuses(occurrenceId: Long, statuses: Set<BookingStatus>): List<Booking>
    fun clear()
}
