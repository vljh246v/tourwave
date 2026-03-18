package com.demo.tourwave.adapter.out.persistence.jpa

import com.demo.tourwave.TourwaveApplication
import com.demo.tourwave.application.common.port.WorkerJobLockRepository
import com.demo.tourwave.application.user.port.UserRepository
import com.demo.tourwave.domain.user.User
import com.demo.tourwave.support.MysqlTestContainerSupport
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@SpringBootTest(classes = [TourwaveApplication::class])
@ActiveProfiles("mysql")
class RealMysqlContainerSmokeTest : MysqlTestContainerSupport() {
    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var workerJobLockRepository: WorkerJobLockRepository

    @BeforeEach
    fun setUp() {
        workerJobLockRepository.clear()
        userRepository.clear()
    }

    @Test
    fun `real mysql container profile boots and persists lock table`() {
        val user = userRepository.save(User.create(displayName = "Lock Owner", email = "lock@example.com", passwordHash = "hashed"))
        assertEquals("lock@example.com", user.email)

        val acquired = workerJobLockRepository.tryAcquire(
            lockName = "offer-expiration",
            ownerId = "container-worker",
            lockedAtUtc = Instant.parse("2026-03-18T00:00:00Z"),
            leaseExpiresAtUtc = Instant.parse("2026-03-18T00:02:00Z")
        )

        assertTrue(acquired)
        assertEquals(1, workerJobLockRepository.findAll().size)
    }
}
