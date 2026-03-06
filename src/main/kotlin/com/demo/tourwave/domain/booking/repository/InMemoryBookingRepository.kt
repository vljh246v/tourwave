package com.demo.tourwave.domain.booking.repository

import com.demo.tourwave.domain.booking.Booking
import com.demo.tourwave.domain.booking.BookingStatus
import org.springframework.stereotype.Repository
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

@Repository
class InMemoryBookingRepository : BookingRepository {
    private val sequence = AtomicLong(0)
    private val bookings = ConcurrentHashMap<Long, Booking>()

    override fun save(booking: Booking): Booking {
        val bookingId = booking.id ?: sequence.incrementAndGet()
        val saved = booking.copy(id = bookingId)
        bookings[bookingId] = saved
        return saved
    }

    override fun findByOccurrenceAndStatuses(occurrenceId: Long, statuses: Set<BookingStatus>): List<Booking> {
        return bookings.values.filter { it.occurrenceId == occurrenceId && it.status in statuses }
    }

    override fun clear() {
        bookings.clear()
        sequence.set(0)
    }
}
