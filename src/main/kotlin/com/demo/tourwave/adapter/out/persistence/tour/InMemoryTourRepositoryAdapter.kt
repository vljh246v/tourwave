package com.demo.tourwave.adapter.out.persistence.tour

import com.demo.tourwave.application.tour.port.TourRepository
import com.demo.tourwave.domain.tour.Tour
import com.demo.tourwave.domain.tour.TourStatus
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Repository
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

@Repository
@Profile("!mysql & !mysql-test")
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

    override fun findByOrganizationId(organizationId: Long): List<Tour> {
        return tours.values.filter { it.organizationId == organizationId }.sortedBy { it.id }
    }

    override fun findAllPublished(): List<Tour> = tours.values.filter { it.status == TourStatus.PUBLISHED }.sortedBy { it.id }

    override fun clear() {
        sequence.set(0L)
        tours.clear()
    }
}
