package com.demo.tourwave.adapter.out.persistence.jpa.auth

import com.demo.tourwave.TourwaveApplication
import com.demo.tourwave.application.auth.port.AuthRefreshTokenRepository
import com.demo.tourwave.domain.auth.AuthRefreshToken
import com.demo.tourwave.domain.common.DomainException
import com.demo.tourwave.domain.common.ErrorCode
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.time.Instant
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import kotlin.test.assertEquals
import kotlin.test.assertIs

@SpringBootTest(classes = [TourwaveApplication::class])
@ActiveProfiles("mysql-test")
class MysqlAuthRefreshTokenRaceTest {
    @Autowired
    private lateinit var authRefreshTokenRepository: AuthRefreshTokenRepository

    @BeforeEach
    fun setUp() {
        authRefreshTokenRepository.clear()
    }

    @Test
    fun `concurrent rotate on same token allows one success, others throw conflict`() {
        val token = authRefreshTokenRepository.save(
            AuthRefreshToken(
                userId = 100L,
                tokenHash = "hash123",
                expiresAtUtc = Instant.parse("2026-05-01T00:00:00Z"),
                issuedAtUtc = Instant.parse("2026-04-26T00:00:00Z"),
            ),
        )

        val threadCount = 10
        val executor = Executors.newFixedThreadPool(threadCount)
        val latch = CountDownLatch(1)

        try {
            val futures = (1..threadCount).map {
                executor.submit(
                    Callable {
                        latch.await()
                        try {
                            authRefreshTokenRepository.rotate(token)
                            "success"
                        } catch (e: DomainException) {
                            if (e.errorCode == ErrorCode.REFRESH_TOKEN_ROTATION_CONFLICT) {
                                "conflict"
                            } else {
                                throw e
                            }
                        }
                    },
                )
            }

            latch.countDown()
            val results = futures.map { it.get() }

            val successCount = results.count { it == "success" }
            val conflictCount = results.count { it == "conflict" }

            assertEquals(1, successCount, "exactly one rotate should succeed")
            assertEquals(threadCount - 1, conflictCount, "all other rotates should conflict")
        } finally {
            executor.shutdown()
        }
    }
}
