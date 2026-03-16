package com.demo.tourwave.adapter.out.persistence.topology

import com.demo.tourwave.application.topology.port.TourRepository
import com.demo.tourwave.domain.tour.Tour
import org.springframework.stereotype.Repository
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

@Repository
class InMemoryTourRepositoryAdapter : TourRepository {
    private val sequence = AtomicLong(0L)
    private val tours = ConcurrentHashMap<Long, Tour>()

    override fun save(tour: Tour): Tour {
        val tourId = tour.id ?: sequence.incrementAndGet()
        val saved = tour.copy(id = tourId)
        tours[tourId] = saved
        return saved
    }

    override fun findById(tourId: Long): Tour? = tours[tourId]

    override fun clear() {
        sequence.set(0L)
        tours.clear()
    }
}
