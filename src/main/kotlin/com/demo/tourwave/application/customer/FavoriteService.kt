package com.demo.tourwave.application.customer

import com.demo.tourwave.application.customer.port.FavoriteRepository
import com.demo.tourwave.application.tour.port.TourRepository
import com.demo.tourwave.application.user.port.UserRepository
import com.demo.tourwave.domain.common.DomainException
import com.demo.tourwave.domain.common.ErrorCode
import com.demo.tourwave.domain.customer.Favorite
import com.demo.tourwave.domain.tour.Tour
import com.demo.tourwave.domain.tour.TourStatus
import java.time.Clock

data class FavoriteView(
    val favoriteId: Long,
    val tourId: Long,
    val organizationId: Long,
    val title: String,
    val summary: String?,
    val attachmentAssetIds: List<Long>,
    val createdAt: java.time.Instant
)

class FavoriteService(
    private val favoriteRepository: FavoriteRepository,
    private val tourRepository: TourRepository,
    private val userRepository: UserRepository,
    private val clock: Clock
) {
    fun favorite(actorUserId: Long, tourId: Long): Favorite {
        requireUser(actorUserId)
        val tour = requirePublishedTour(tourId)
        return favoriteRepository.findByUserIdAndTourId(actorUserId, tour.id!!)
            ?: favoriteRepository.save(
                Favorite(
                    userId = actorUserId,
                    tourId = tour.id,
                    createdAt = clock.instant()
                )
            )
    }

    fun unfavorite(actorUserId: Long, tourId: Long) {
        requireUser(actorUserId)
        favoriteRepository.findByUserIdAndTourId(actorUserId, tourId)?.let { favoriteRepository.delete(requireNotNull(it.id)) }
    }

    fun list(actorUserId: Long): List<FavoriteView> {
        requireUser(actorUserId)
        return favoriteRepository.findByUserId(actorUserId)
            .mapNotNull { favorite ->
                val tour = tourRepository.findById(favorite.tourId)
                if (tour == null || tour.status != TourStatus.PUBLISHED) {
                    null
                } else {
                    FavoriteView(
                        favoriteId = requireNotNull(favorite.id),
                        tourId = requireNotNull(tour.id),
                        organizationId = tour.organizationId,
                        title = tour.title,
                        summary = tour.summary,
                        attachmentAssetIds = tour.attachmentAssetIds,
                        createdAt = favorite.createdAt
                    )
                }
            }
    }

    private fun requirePublishedTour(tourId: Long): Tour {
        val tour = tourRepository.findById(tourId) ?: throw notFound("tour $tourId not found")
        if (tour.status != TourStatus.PUBLISHED) {
            throw notFound("tour $tourId not found")
        }
        return tour
    }

    private fun requireUser(actorUserId: Long) {
        userRepository.findById(actorUserId) ?: throw DomainException(
            errorCode = ErrorCode.UNAUTHORIZED,
            status = 401,
            message = "authenticated user does not exist"
        )
    }

    private fun notFound(message: String) = DomainException(
        errorCode = ErrorCode.VALIDATION_ERROR,
        status = 404,
        message = message
    )
}
