package com.demo.tourwave.application.customer

import com.demo.tourwave.adapter.out.persistence.customer.InMemoryFavoriteRepositoryAdapter
import com.demo.tourwave.domain.tour.TourStatus
import com.demo.tourwave.domain.user.User
import com.demo.tourwave.support.FakeTourRepository
import com.demo.tourwave.support.FakeUserRepository
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.assertEquals

class FavoriteServiceTest {
    private val favoriteRepository = InMemoryFavoriteRepositoryAdapter()
    private val tourRepository = FakeTourRepository()
    private val userRepository = FakeUserRepository()
    private val service = FavoriteService(
        favoriteRepository = favoriteRepository,
        tourRepository = tourRepository,
        userRepository = userRepository,
        clock = Clock.fixed(Instant.parse("2026-03-18T00:00:00Z"), ZoneOffset.UTC)
    )

    @Test
    fun `favorite list and unfavorite work against published tours`() {
        userRepository.save(User.create(displayName = "Fav", email = "fav@test.com", passwordHash = "hash"))
        tourRepository.save(
            com.demo.tourwave.domain.tour.Tour.create(
                organizationId = 1L,
                title = "Favorite Tour",
                summary = "Summary",
                now = Instant.parse("2026-03-18T00:00:00Z")
            ).copy(id = 10L, status = TourStatus.PUBLISHED, attachmentAssetIds = listOf(90L))
        )

        service.favorite(1L, 10L)
        val favorites = service.list(1L)
        service.unfavorite(1L, 10L)

        assertEquals(1, favorites.size)
        assertEquals(listOf(90L), favorites.single().attachmentAssetIds)
        assertEquals(0, service.list(1L).size)
    }
}
