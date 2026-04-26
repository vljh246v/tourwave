package com.demo.tourwave.application.tour

import com.demo.tourwave.application.common.port.AuditEventCommand
import com.demo.tourwave.application.common.port.AuditEventPort
import com.demo.tourwave.application.organization.OrganizationAccessGuard
import com.demo.tourwave.application.organization.port.OrganizationRepository
import com.demo.tourwave.application.tour.port.TourRepository
import com.demo.tourwave.domain.common.DomainException
import com.demo.tourwave.domain.common.ErrorCode
import com.demo.tourwave.domain.tour.Tour
import com.demo.tourwave.domain.tour.TourContent
import org.springframework.transaction.annotation.Transactional
import java.time.Clock

@Transactional
class TourCommandService(
    private val tourRepository: TourRepository,
    private val organizationRepository: OrganizationRepository,
    private val organizationAccessGuard: OrganizationAccessGuard,
    private val auditEventPort: AuditEventPort,
    private val clock: Clock,
) {
    fun create(command: CreateTourCommand): Tour {
        organizationRepository.findById(command.organizationId) ?: throw organizationNotFound(command.organizationId)
        organizationAccessGuard.requireOperator(command.actorUserId, command.organizationId)
        val saved =
            tourRepository.save(
                Tour.create(
                    organizationId = command.organizationId,
                    title = requireValidTourTitle(command.title),
                    summary = normalizeOptionalTourSummary(command.summary),
                    now = clock.instant(),
                ),
            )
        auditEventPort.append(
            AuditEventCommand(
                actor = "OPERATOR:${command.actorUserId}",
                action = "TOUR_CREATED",
                resourceType = "TOUR",
                resourceId = requireNotNull(saved.id),
                occurredAtUtc = clock.instant(),
                reasonCode = "TOUR_CREATED",
                afterJson = tourSnapshot(saved),
            ),
        )
        return saved
    }

    fun update(command: UpdateTourCommand): Tour {
        val tour = requireTour(command.tourId)
        organizationAccessGuard.requireOperator(command.actorUserId, tour.organizationId)
        val saved =
            tourRepository.save(
                tour.updateMetadata(
                    title = requireValidTourTitle(command.title),
                    summary = normalizeOptionalTourSummary(command.summary),
                    now = clock.instant(),
                ),
            )
        auditEventPort.append(
            AuditEventCommand(
                actor = "OPERATOR:${command.actorUserId}",
                action = "TOUR_UPDATED",
                resourceType = "TOUR",
                resourceId = requireNotNull(saved.id),
                occurredAtUtc = clock.instant(),
                reasonCode = "TOUR_UPDATED",
                beforeJson = tourSnapshot(tour),
                afterJson = tourSnapshot(saved),
            ),
        )
        return saved
    }

    fun updateContent(command: UpdateTourContentCommand): Tour {
        val tour = requireTour(command.tourId)
        organizationAccessGuard.requireOperator(command.actorUserId, tour.organizationId)
        val saved =
            tourRepository.save(
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
        auditEventPort.append(
            AuditEventCommand(
                actor = "OPERATOR:${command.actorUserId}",
                action = "TOUR_UPDATED",
                resourceType = "TOUR",
                resourceId = requireNotNull(saved.id),
                occurredAtUtc = clock.instant(),
                reasonCode = "TOUR_UPDATED",
                beforeJson = tourSnapshot(tour),
                afterJson = tourSnapshot(saved),
            ),
        )
        return saved
    }

    fun publish(command: PublishTourCommand): Tour {
        val tour = requireTour(command.tourId)
        organizationAccessGuard.requireOperator(command.actorUserId, tour.organizationId)
        val saved = tourRepository.save(tour.publish(clock.instant()))
        auditEventPort.append(
            AuditEventCommand(
                actor = "OPERATOR:${command.actorUserId}",
                action = "TOUR_PUBLISHED",
                resourceType = "TOUR",
                resourceId = requireNotNull(saved.id),
                occurredAtUtc = clock.instant(),
                reasonCode = "TOUR_PUBLISHED",
                beforeJson = tourSnapshot(tour),
                afterJson = tourSnapshot(saved),
            ),
        )
        return saved
    }

    fun archive(command: ArchiveTourCommand): Tour {
        val tour = requireTour(command.tourId)
        organizationAccessGuard.requireOperator(command.actorUserId, tour.organizationId)
        val archived =
            try {
                tour.archive(clock.instant())
            } catch (e: IllegalStateException) {
                throw DomainException(
                    errorCode = ErrorCode.INVALID_STATE_TRANSITION,
                    status = 409,
                    message = e.message ?: "invalid state transition for archive",
                )
            }
        // idempotent no-op: if already archived, skip save + audit
        if (archived === tour) return tour
        val saved = tourRepository.save(archived)
        auditEventPort.append(
            AuditEventCommand(
                actor = "OPERATOR:${command.actorUserId}",
                action = "TOUR_ARCHIVED",
                resourceType = "TOUR",
                resourceId = requireNotNull(saved.id),
                occurredAtUtc = clock.instant(),
                reasonCode = "TOUR_ARCHIVED",
                beforeJson = tourSnapshot(tour),
                afterJson = tourSnapshot(saved),
            ),
        )
        return saved
    }

    private fun tourSnapshot(tour: Tour): Map<String, Any?> =
        mapOf(
            "organizationId" to tour.organizationId,
            "title" to tour.title,
            "status" to tour.status.name,
        )

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
