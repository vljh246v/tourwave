package com.demo.tourwave.adapter.`in`.web.tour

import com.demo.tourwave.adapter.`in`.web.occurrence.OccurrenceResponse
import com.demo.tourwave.adapter.`in`.web.occurrence.toResponse
import com.demo.tourwave.application.topology.CatalogQueryService
import com.demo.tourwave.application.topology.PublicTourListQuery
import com.demo.tourwave.application.topology.TourOccurrenceListQuery
import com.demo.tourwave.application.topology.TourQueryService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
class TourPublicController(
    private val catalogQueryService: CatalogQueryService,
    private val tourQueryService: TourQueryService
) {
    @GetMapping("/tours")
    fun listTours(
        @RequestParam(required = false) cursor: String?,
        @RequestParam(required = false) limit: Int?,
        @RequestParam(required = false) q: String?,
        @RequestParam(required = false) organizationId: Long?
    ): ResponseEntity<TourCatalogListResponse> {
        val (items, nextCursor) = catalogQueryService.listPublicTours(
            PublicTourListQuery(
                cursor = cursor,
                limit = limit,
                q = q,
                organizationId = organizationId
            )
        )
        return ResponseEntity.ok(
            TourCatalogListResponse(
                items = items.map { it.toResponse() },
                nextCursor = nextCursor
            )
        )
    }

    @GetMapping("/tours/{tourId}")
    fun getTour(@PathVariable tourId: Long): ResponseEntity<PublicTourResponse> {
        return ResponseEntity.ok(catalogQueryService.getPublicTour(tourId).toResponse())
    }

    @GetMapping("/tours/{tourId}/occurrences")
    fun listOccurrences(
        @PathVariable tourId: Long,
        @RequestParam(required = false) dateFrom: Instant?,
        @RequestParam(required = false) dateTo: Instant?
    ): ResponseEntity<List<OccurrenceResponse>> {
        return ResponseEntity.ok(
            catalogQueryService.listPublicOccurrences(
                TourOccurrenceListQuery(
                    tourId = tourId,
                    dateFrom = dateFrom,
                    dateTo = dateTo
                )
            ).map { it.toResponse() }
        )
    }

    @GetMapping("/tours/{tourId}/content")
    fun getPublicContent(@PathVariable tourId: Long): ResponseEntity<TourContentResponse> {
        return ResponseEntity.ok(tourQueryService.getPublicContent(tourId).toResponse())
    }
}
