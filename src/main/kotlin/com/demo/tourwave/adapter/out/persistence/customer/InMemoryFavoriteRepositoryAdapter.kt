package com.demo.tourwave.adapter.out.persistence.customer

import com.demo.tourwave.application.customer.port.FavoriteRepository
import com.demo.tourwave.domain.customer.Favorite
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Repository
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

@Repository
@Profile("!mysql & !mysql-test")
class InMemoryFavoriteRepositoryAdapter : FavoriteRepository {
    private val sequence = AtomicLong(0)
    private val favorites = ConcurrentHashMap<Long, Favorite>()

    override fun save(favorite: Favorite): Favorite {
        val existing = findByUserIdAndTourId(favorite.userId, favorite.tourId)
        if (existing != null) return existing
        val favoriteId = favorite.id ?: sequence.incrementAndGet()
        val saved = favorite.copy(id = favoriteId)
        favorites[favoriteId] = saved
        return saved
    }

    override fun findByUserId(userId: Long): List<Favorite> =
        favorites.values.filter { it.userId == userId }.sortedByDescending { it.createdAt }

    override fun findByUserIdAndTourId(userId: Long, tourId: Long): Favorite? =
        favorites.values.firstOrNull { it.userId == userId && it.tourId == tourId }

    override fun delete(favoriteId: Long) {
        favorites.remove(favoriteId)
    }

    override fun clear() {
        favorites.clear()
        sequence.set(0)
    }
}
