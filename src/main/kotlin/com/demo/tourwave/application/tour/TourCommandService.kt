package com.demo.tourwave.application.tour

import com.demo.tourwave.application.organization.OrganizationAccessGuard
import com.demo.tourwave.application.organization.port.OrganizationRepository
import com.demo.tourwave.application.tour.port.TourRepository
import com.demo.tourwave.domain.common.DomainException
import com.demo.tourwave.domain.common.ErrorCode
import com.demo.tourwave.domain.tour.Tour
import com.demo.tourwave.domain.tour.TourContent
import java.time.Clock

class TourCommandService(
    private val tourRepository: TourRepository,
    private val organizationRepository: OrganizationRepository,
    private val organizationAccessGuard: OrganizationAccessGuard,
    private val clock: Clock,
) {
    fun create(command: CreateTourCommand): Tour {
        organizationRepository.findById(command.organizationId) ?: throw organizationNotFound(command.organizationId)
        organizationAccessGuard.requireOperator(command.actorUserId, command.organizationId)
        return tourRepository.save(
            Tour.create(
                organizationId = command.organizationId,
                title = requireValidTourTitle(command.title),
                summary = normalizeOptionalTourSummary(command.summary),
                now = clock.instant(),
            ),
        )
    }

    fun update(command: UpdateTourCommand): Tour {
        val tour = requireTour(command.tourId)
        organizationAccessGuard.requireOperator(command.actorUserId, tour.organizationId)
        return tourRepository.save(
            tour.updateMetadata(
                title = requireValidTourTitle(command.title),
                summary = normalizeOptionalTourSummary(command.summary),
                now = clock.instant(),
            ),
        )
    }

    fun updateContent(command: UpdateTourContentCommand): Tour {
        val tour = requireTour(command.tourId)
        organizationAccessGuard.requireOperator(command.actorUserId, tour.organizationId)
        return tourRepository.save(
            tour.updateContent(
                content =
                    TourContent(
                        description = normalizeOptionalTourDescription(command.description),
                        highlights = normalizeStringList(command.highlights, "highlights", maxItems = 20, maxLength = 160),
                        inclusions = normalizeStringList(command.inclusions, "inclusions", maxItems = 20, maxLength = 160),
                        exclusions = normalizeStringList(command.exclusions, "exclusions", maxItems = 20, maxLength = 160),
                        preparations = normalizeStringList(command.preparations, "preparations", maxItems = 20, maxLength = 160),
                        policies = normalizeStringList(command.policies, "policies", maxItems = 20, maxLength = 160),
                    ),
                now = clock.instant(),
            ),
        )
    }

    fun publish(command: PublishTourCommand): Tour {
        val tour = requireTour(command.tourId)
        organizationAccessGuard.requireOperator(command.actorUserId, tour.organizationId)
        return tourRepository.save(tour.publish(clock.instant()))
    }

    private fun requireTour(tourId: Long): Tour =
        tourRepository.findById(tourId) ?: throw DomainException(
            errorCode = ErrorCode.VALIDATION_ERROR,
            status = 404,
            message = "tour $tourId not found",
        )

    private fun organizationNotFound(organizationId: Long) =
        DomainException(
            errorCode = ErrorCode.VALIDATION_ERROR,
            status = 404,
            message = "organization $organizationId not found",
        )
}
