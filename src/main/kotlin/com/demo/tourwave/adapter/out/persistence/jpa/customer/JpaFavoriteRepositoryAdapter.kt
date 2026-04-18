package com.demo.tourwave.adapter.out.persistence.jpa.customer

import com.demo.tourwave.application.customer.port.FavoriteRepository
import com.demo.tourwave.domain.customer.Favorite
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Repository

@Repository
@Profile("mysql", "mysql-test")
class JpaFavoriteRepositoryAdapter(
    private val favoriteJpaRepository: FavoriteJpaRepository,
) : FavoriteRepository {
    override fun save(favorite: Favorite): Favorite {
        val existing = favoriteJpaRepository.findByUserIdAndTourId(favorite.userId, favorite.tourId)
        return existing?.toDomain() ?: favoriteJpaRepository.save(favorite.toEntity()).toDomain()
    }

    override fun findByUserId(userId: Long): List<Favorite> = favoriteJpaRepository.findByUserIdOrderByCreatedAtDesc(userId).map { it.toDomain() }

    override fun findByUserIdAndTourId(
        userId: Long,
        tourId: Long,
    ): Favorite? = favoriteJpaRepository.findByUserIdAndTourId(userId, tourId)?.toDomain()

    override fun delete(favoriteId: Long) {
        favoriteJpaRepository.deleteById(favoriteId)
    }

    override fun clear() {
        favoriteJpaRepository.deleteAllInBatch()
    }
}

private fun Favorite.toEntity(): FavoriteJpaEntity =
    FavoriteJpaEntity(
        id = id,
        userId = userId,
        tourId = tourId,
        createdAt = createdAt,
    )

private fun FavoriteJpaEntity.toDomain(): Favorite =
    Favorite(
        id = id,
        userId = userId,
        tourId = tourId,
        createdAt = createdAt,
    )
