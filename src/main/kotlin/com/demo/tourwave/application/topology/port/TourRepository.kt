package com.demo.tourwave.application.topology.port

import com.demo.tourwave.domain.tour.Tour

interface TourRepository {
    fun save(tour: Tour): Tour
    fun findById(tourId: Long): Tour?
    fun clear()
}
