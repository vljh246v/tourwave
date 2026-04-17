package com.demo.tourwave.adapter.`in`.web.occurrence

import com.demo.tourwave.application.occurrence.AvailabilityQuery
import com.demo.tourwave.application.occurrence.CatalogQueryService
import com.demo.tourwave.application.occurrence.OccurrenceSearchQuery
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
class OccurrencePublicController(
    private val catalogQueryService: CatalogQueryService,
) {
    @GetMapping("/occurrences/{occurrenceId}")
    fun getOccurrence(
        @PathVariable occurrenceId: Long,
    ): ResponseEntity<OccurrenceResponse> {
        return ResponseEntity.ok(catalogQueryService.getPublicOccurrence(occurrenceId).toResponse())
    }

    @GetMapping("/occurrences/{occurrenceId}/availability")
    fun getAvailability(
        @PathVariable occurrenceId: Long,
        @RequestParam partySize: Int,
    ): ResponseEntity<AvailabilityResponse> {
        return ResponseEntity.ok(
            catalogQueryService.getAvailability(
                AvailabilityQuery(
                    occurrenceId = occurrenceId,
                    partySize = partySize,
                ),
            ).toResponse(),
        )
    }

    @GetMapping("/occurrences/{occurrenceId}/quote")
    fun getQuote(
        @PathVariable occurrenceId: Long,
        @RequestParam partySize: Int,
    ): ResponseEntity<QuoteResponse> {
        return ResponseEntity.ok(
            catalogQueryService.getQuote(
                AvailabilityQuery(
                    occurrenceId = occurrenceId,
                    partySize = partySize,
                ),
            ).toResponse(),
        )
    }

    @GetMapping("/search/occurrences")
    fun search(
        @RequestParam(required = false) cursor: String?,
        @RequestParam(required = false) limit: Int?,
        @RequestParam(required = false) dateFrom: Instant?,
        @RequestParam(required = false) dateTo: Instant?,
        @RequestParam(required = false) timezone: String?,
        @RequestParam(required = false) locationText: String?,
        @RequestParam(required = false) partySize: Int?,
        @RequestParam(required = false, defaultValue = "false") onlyAvailable: Boolean,
        @RequestParam(required = false) sort: String?,
    ): ResponseEntity<OccurrenceSearchResponse> {
        return ResponseEntity.ok(
            catalogQueryService.search(
                OccurrenceSearchQuery(
                    cursor = cursor,
                    limit = limit,
                    dateFrom = dateFrom,
                    dateTo = dateTo,
                    timezone = timezone,
                    locationText = locationText,
                    partySize = partySize,
                    onlyAvailable = onlyAvailable,
                    sort = sort,
                ),
            ).toResponse(),
        )
    }
}
