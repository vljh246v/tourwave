package com.demo.tourwave.adapter.`in`.web.tour

import com.demo.tourwave.application.common.port.AuthzGuardPort
import com.demo.tourwave.application.tour.CreateTourCommand
import com.demo.tourwave.application.tour.PublishTourCommand
import com.demo.tourwave.application.tour.TourCommandService
import com.demo.tourwave.application.tour.TourQueryService
import com.demo.tourwave.application.tour.UpdateTourCommand
import com.demo.tourwave.application.tour.UpdateTourContentCommand
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController

@RestController
class TourOperatorController(
    private val tourCommandService: TourCommandService,
    private val tourQueryService: TourQueryService,
    private val authzGuardPort: AuthzGuardPort
) {
    @PostMapping("/organizations/{organizationId}/tours")
    fun createTour(
        @PathVariable organizationId: Long,
        @RequestHeader("X-Actor-User-Id", required = false) actorUserId: Long?,
        @RequestBody request: CreateTourWebRequest
    ): ResponseEntity<TourResponse> {
        val requiredActorUserId = authzGuardPort.requireActorUserId(actorUserId)
        return ResponseEntity.status(201).body(
            tourCommandService.create(
                CreateTourCommand(
                    actorUserId = requiredActorUserId,
                    organizationId = organizationId,
                    title = request.title,
                    summary = request.summary
                )
            ).toResponse()
        )
    }

    @GetMapping("/organizations/{organizationId}/tours")
    fun listTours(
        @PathVariable organizationId: Long,
        @RequestHeader("X-Actor-User-Id", required = false) actorUserId: Long?
    ): ResponseEntity<List<TourResponse>> {
        val requiredActorUserId = authzGuardPort.requireActorUserId(actorUserId)
        return ResponseEntity.ok(
            tourQueryService.listByOrganization(requiredActorUserId, organizationId).map { it.toResponse() }
        )
    }

    @PatchMapping("/tours/{tourId}")
    fun updateTour(
        @PathVariable tourId: Long,
        @RequestHeader("X-Actor-User-Id", required = false) actorUserId: Long?,
        @RequestBody request: UpdateTourWebRequest
    ): ResponseEntity<TourResponse> {
        val requiredActorUserId = authzGuardPort.requireActorUserId(actorUserId)
        return ResponseEntity.ok(
            tourCommandService.update(
                UpdateTourCommand(
                    actorUserId = requiredActorUserId,
                    tourId = tourId,
                    title = request.title,
                    summary = request.summary
                )
            ).toResponse()
        )
    }

    @PostMapping("/tours/{tourId}/publish")
    fun publishTour(
        @PathVariable tourId: Long,
        @RequestHeader("X-Actor-User-Id", required = false) actorUserId: Long?
    ): ResponseEntity<TourResponse> {
        val requiredActorUserId = authzGuardPort.requireActorUserId(actorUserId)
        return ResponseEntity.ok(
            tourCommandService.publish(
                PublishTourCommand(
                    actorUserId = requiredActorUserId,
                    tourId = tourId
                )
            ).toResponse()
        )
    }

    @PutMapping("/tours/{tourId}/content")
    fun updateTourContent(
        @PathVariable tourId: Long,
        @RequestHeader("X-Actor-User-Id", required = false) actorUserId: Long?,
        @RequestBody request: UpdateTourContentWebRequest
    ): ResponseEntity<TourContentResponse> {
        val requiredActorUserId = authzGuardPort.requireActorUserId(actorUserId)
        val tour = tourCommandService.updateContent(
            UpdateTourContentCommand(
                actorUserId = requiredActorUserId,
                tourId = tourId,
                description = request.description,
                highlights = request.highlights,
                inclusions = request.inclusions,
                exclusions = request.exclusions,
                preparations = request.preparations,
                policies = request.policies
            )
        )
        return ResponseEntity.ok(tour.content.toResponse())
    }
}
