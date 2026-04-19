package com.demo.tourwave.application.customer.port

import com.demo.tourwave.domain.customer.Favorite

interface FavoriteRepository {
    fun save(favorite: Favorite): Favorite

    fun findByUserId(userId: Long): List<Favorite>

    fun findByUserIdAndTourId(
        userId: Long,
        tourId: Long,
    ): Favorite?

    fun delete(favoriteId: Long)

    fun clear()
}
