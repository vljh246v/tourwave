package com.demo.tourwave.application.tour

import com.demo.tourwave.application.organization.OrganizationAccessGuard
import com.demo.tourwave.application.tour.port.TourRepository
import com.demo.tourwave.domain.common.DomainException
import com.demo.tourwave.domain.common.ErrorCode
import com.demo.tourwave.domain.tour.Tour
import com.demo.tourwave.domain.tour.TourContent
import com.demo.tourwave.domain.tour.TourStatus

class TourQueryService(
    private val tourRepository: TourRepository,
    private val organizationAccessGuard: OrganizationAccessGuard
) {
    fun listByOrganization(actorUserId: Long, organizationId: Long): List<Tour> {
        organizationAccessGuard.requireMembership(actorUserId, organizationId)
        return tourRepository.findByOrganizationId(organizationId)
    }

    fun getPublicContent(tourId: Long): TourContent {
        val tour = tourRepository.findById(tourId) ?: throw notFound(tourId)
        if (tour.status != TourStatus.PUBLISHED) {
            throw notFound(tourId)
        }
        return tour.content
    }

    private fun notFound(tourId: Long) = DomainException(
        errorCode = ErrorCode.VALIDATION_ERROR,
        status = 404,
        message = "tour $tourId not found"
    )
}
