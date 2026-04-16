package com.demo.tourwave.adapter.out.persistence.jpa

import com.demo.tourwave.TourwaveApplication
import com.demo.tourwave.application.booking.port.BookingRepository
import com.demo.tourwave.application.booking.port.OccurrenceRepository
import com.demo.tourwave.application.booking.port.PaymentRecordRepository
import com.demo.tourwave.application.common.port.IdempotencyDecision
import com.demo.tourwave.application.common.port.IdempotencyStore
import com.demo.tourwave.application.common.port.WorkerJobLockRepository
import com.demo.tourwave.application.inquiry.port.InquiryRepository
import com.demo.tourwave.application.operations.port.OperatorFailureRecordRepository
import com.demo.tourwave.application.participant.port.BookingParticipantRepository
import com.demo.tourwave.application.review.port.ReviewRepository
import com.demo.tourwave.application.auth.port.AuthRefreshTokenRepository
import com.demo.tourwave.application.auth.port.UserActionTokenRepository
import com.demo.tourwave.application.asset.port.AssetRepository
import com.demo.tourwave.application.customer.port.FavoriteRepository
import com.demo.tourwave.application.customer.port.NotificationRepository
import com.demo.tourwave.application.payment.port.PaymentProviderEventRepository
import com.demo.tourwave.application.payment.port.PaymentReconciliationSummaryRepository
import com.demo.tourwave.application.instructor.port.InstructorProfileRepository
import com.demo.tourwave.application.instructor.port.InstructorRegistrationRepository
import com.demo.tourwave.application.topology.port.OrganizationMembershipRepository
import com.demo.tourwave.application.topology.port.OrganizationRepository
import com.demo.tourwave.application.tour.port.TourRepository
import com.demo.tourwave.application.user.port.UserRepository
import com.demo.tourwave.domain.booking.AttendanceStatus
import com.demo.tourwave.domain.booking.Booking
import com.demo.tourwave.domain.booking.BookingStatus
import com.demo.tourwave.domain.booking.PaymentStatus
import com.demo.tourwave.domain.asset.Asset
import com.demo.tourwave.domain.asset.AssetStatus
import com.demo.tourwave.domain.customer.Favorite
import com.demo.tourwave.domain.customer.Notification
import com.demo.tourwave.domain.customer.NotificationType
import com.demo.tourwave.domain.instructor.InstructorProfile
import com.demo.tourwave.domain.instructor.InstructorProfileStatus
import com.demo.tourwave.domain.instructor.InstructorRegistration
import com.demo.tourwave.domain.instructor.InstructorRegistrationStatus
import com.demo.tourwave.domain.inquiry.Inquiry
import com.demo.tourwave.domain.inquiry.InquiryMessage
import com.demo.tourwave.domain.occurrence.Occurrence
import com.demo.tourwave.domain.operations.OperatorFailureAction
import com.demo.tourwave.domain.operations.OperatorFailureRecord
import com.demo.tourwave.domain.operations.OperatorFailureRecordStatus
import com.demo.tourwave.domain.operations.OperatorFailureSourceType
import com.demo.tourwave.domain.participant.BookingParticipant
import com.demo.tourwave.domain.payment.PaymentRecord
import com.demo.tourwave.domain.payment.PaymentProviderEvent
import com.demo.tourwave.domain.payment.PaymentProviderEventStatus
import com.demo.tourwave.domain.payment.PaymentProviderEventType
import com.demo.tourwave.domain.payment.PaymentRecordStatus
import com.demo.tourwave.domain.payment.PaymentReconciliationDailySummary
import com.demo.tourwave.domain.organization.Organization
import com.demo.tourwave.domain.organization.OrganizationMembership
import com.demo.tourwave.domain.organization.OrganizationRole
import com.demo.tourwave.domain.review.Review
import com.demo.tourwave.domain.review.ReviewType
import com.demo.tourwave.domain.tour.Tour
import com.demo.tourwave.domain.tour.TourContent
import com.demo.tourwave.domain.tour.TourStatus
import com.demo.tourwave.domain.user.User
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@SpringBootTest(classes = [TourwaveApplication::class])
@ActiveProfiles("mysql-test")
class MysqlPersistenceIntegrationTest {
    @Autowired
    private lateinit var occurrenceRepository: OccurrenceRepository

    @Autowired
    private lateinit var bookingRepository: BookingRepository

    @Autowired
    private lateinit var paymentRecordRepository: PaymentRecordRepository

    @Autowired
    private lateinit var bookingParticipantRepository: BookingParticipantRepository

    @Autowired
    private lateinit var inquiryRepository: InquiryRepository

    @Autowired
    private lateinit var reviewRepository: ReviewRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var organizationRepository: OrganizationRepository

    @Autowired
    private lateinit var organizationMembershipRepository: OrganizationMembershipRepository

    @Autowired
    private lateinit var instructorRegistrationRepository: InstructorRegistrationRepository

    @Autowired
    private lateinit var instructorProfileRepository: InstructorProfileRepository

    @Autowired
    private lateinit var tourRepository: TourRepository

    @Autowired
    private lateinit var idempotencyStore: IdempotencyStore

    @Autowired
    private lateinit var authRefreshTokenRepository: AuthRefreshTokenRepository

    @Autowired
    private lateinit var userActionTokenRepository: UserActionTokenRepository

    @Autowired
    private lateinit var assetRepository: AssetRepository

    @Autowired
    private lateinit var favoriteRepository: FavoriteRepository

    @Autowired
    private lateinit var notificationRepository: NotificationRepository

    @Autowired
    private lateinit var paymentProviderEventRepository: PaymentProviderEventRepository

    @Autowired
    private lateinit var paymentReconciliationSummaryRepository: PaymentReconciliationSummaryRepository

    @Autowired
    private lateinit var workerJobLockRepository: WorkerJobLockRepository

    @Autowired
    private lateinit var operatorFailureRecordRepository: OperatorFailureRecordRepository

    @BeforeEach
    fun setUp() {
        operatorFailureRecordRepository.clear()
        workerJobLockRepository.clear()
        paymentReconciliationSummaryRepository.clear()
        paymentProviderEventRepository.clear()
        notificationRepository.clear()
        favoriteRepository.clear()
        assetRepository.clear()
        reviewRepository.clear()
        inquiryRepository.clear()
        bookingParticipantRepository.clear()
        paymentRecordRepository.clear()
        bookingRepository.clear()
        occurrenceRepository.clear()
        tourRepository.clear()
        instructorProfileRepository.clear()
        instructorRegistrationRepository.clear()
        organizationMembershipRepository.clear()
        organizationRepository.clear()
        userRepository.clear()
        authRefreshTokenRepository.clear()
        userActionTokenRepository.clear()
        idempotencyStore.clear()
    }

    @Test
    fun `mysql adapters persist booking aggregate and related views`() {
        occurrenceRepository.save(
            Occurrence(
                id = 9101L,
                organizationId = 31L,
                tourId = 99L,
                instructorProfileId = 77L,
                capacity = 10,
                startsAtUtc = Instant.parse("2026-03-20T09:00:00Z"),
                endsAtUtc = Instant.parse("2026-03-20T11:00:00Z"),
                timezone = "Asia/Seoul",
                unitPrice = 55000,
                currency = "KRW",
                locationText = "Myeongdong",
                meetingPoint = "Gate A",
                createdAt = Instant.parse("2026-03-10T00:00:00Z"),
                updatedAt = Instant.parse("2026-03-11T00:00:00Z"),
            ),
        )
        val booking =
            bookingRepository.save(
                Booking(
                    occurrenceId = 9101L,
                    organizationId = 31L,
                    leaderUserId = 501L,
                    partySize = 2,
                    status = BookingStatus.CONFIRMED,
                    paymentStatus = PaymentStatus.PAID,
                    createdAt = Instant.parse("2026-03-12T00:00:00Z"),
                ),
            )
        val bookingId = requireNotNull(booking.id)
        paymentRecordRepository.save(
            PaymentRecord(
                bookingId = bookingId,
                status = PaymentRecordStatus.CAPTURED,
                createdAtUtc = Instant.parse("2026-03-12T00:00:00Z"),
                updatedAtUtc = Instant.parse("2026-03-12T00:05:00Z"),
            ),
        )
        bookingParticipantRepository.save(
            BookingParticipant
                .leader(
                    bookingId = bookingId,
                    userId = 501L,
                    createdAt = Instant.parse("2026-03-12T00:00:00Z"),
                ).recordAttendance(AttendanceStatus.ATTENDED),
        )
        reviewRepository.save(
            Review(
                occurrenceId = 9101L,
                reviewerUserId = 501L,
                type = ReviewType.TOUR,
                rating = 5,
                comment = "great",
                createdAt = Instant.parse("2026-03-21T00:00:00Z"),
            ),
        )

        val persisted = bookingRepository.findById(bookingId)
        val persistedOccurrence = occurrenceRepository.findById(9101L)
        val payment = paymentRecordRepository.findByBookingId(bookingId)
        val participants = bookingParticipantRepository.findByBookingId(bookingId)
        val reviews = reviewRepository.findByOccurrenceAndType(9101L, ReviewType.TOUR)

        assertNotNull(persisted)
        assertNotNull(persistedOccurrence)
        assertEquals(BookingStatus.CONFIRMED, persisted.status)
        assertEquals(55000, persistedOccurrence.unitPrice)
        assertEquals("Myeongdong", persistedOccurrence.locationText)
        assertEquals(PaymentRecordStatus.CAPTURED, payment?.status)
        assertEquals(1, participants.size)
        assertEquals(AttendanceStatus.ATTENDED, participants.single().attendanceStatus)
        assertEquals(1, reviews.size)
    }

    @Test
    fun `mysql adapters persist inquiry messages users and idempotency replay`() {
        val user = userRepository.save(User.create(displayName = "Jae", email = "JAE@EXAMPLE.COM", passwordHash = "hashed"))
        assertEquals("jae@example.com", user.email)

        val inquiry =
            inquiryRepository.save(
                Inquiry(
                    organizationId = 31L,
                    occurrenceId = 9102L,
                    bookingId = 9202L,
                    createdByUserId = requireNotNull(user.id),
                    subject = "Need pickup",
                    createdAt = Instant.parse("2026-03-10T10:00:00Z"),
                ),
            )
        inquiryRepository.saveMessage(
            InquiryMessage(
                inquiryId = requireNotNull(inquiry.id),
                senderUserId = requireNotNull(user.id),
                body = "hello",
                attachmentAssetIds = listOf(11L, 12L),
                createdAt = Instant.parse("2026-03-10T10:01:00Z"),
            ),
        )

        val reserved = idempotencyStore.reserveOrReplay(1L, "POST", "/bookings/{bookingId}/cancel", "idem-1", "abc123")
        assertTrue(reserved is IdempotencyDecision.Reserved)
        idempotencyStore.complete(1L, "POST", "/bookings/{bookingId}/cancel", "idem-1", 204, mapOf("ok" to true))
        val replay = idempotencyStore.reserveOrReplay(1L, "POST", "/bookings/{bookingId}/cancel", "idem-1", "abc123")

        val messages = inquiryRepository.findMessagesByInquiryId(requireNotNull(inquiry.id))
        assertEquals(1, messages.size)
        assertEquals(listOf(11L, 12L), messages.single().attachmentAssetIds)
        assertTrue(replay is IdempotencyDecision.Replay)
        assertEquals(204, replay.status)
    }

    @Test
    fun `mysql adapters persist organizations and memberships`() {
        val owner = userRepository.save(User.create(displayName = "Owner", email = "owner@test.com", passwordHash = "hash"))
        val organization = organizationRepository.save(
            Organization.create(
                slug = "seoul-jpa",
                name = "Seoul JPA",
                description = "operator profile",
                publicDescription = "public profile",
                contactEmail = "ops@seoul.test",
                contactPhone = "+82 10 0000 0000",
                websiteUrl = "https://seoul.test",
                businessName = "Seoul JPA LLC",
                businessRegistrationNumber = "123-45-67890",
                timezone = "Asia/Seoul",
                now = Instant.parse("2026-03-17T00:00:00Z")
            )
        )
        organizationMembershipRepository.save(
            OrganizationMembership.active(
                organizationId = requireNotNull(organization.id),
                userId = requireNotNull(owner.id),
                role = OrganizationRole.OWNER,
                now = Instant.parse("2026-03-17T00:00:00Z")
            )
        )

        val persistedOrganization = organizationRepository.findBySlug("seoul-jpa")
        val memberships = organizationMembershipRepository.findByOrganizationId(requireNotNull(organization.id))

        assertNotNull(persistedOrganization)
        assertEquals("Seoul JPA", persistedOrganization.name)
        assertEquals(1, memberships.size)
        assertEquals(OrganizationRole.OWNER, memberships.single().role)
    }

    @Test
    fun `mysql adapters persist instructor registrations instructor profiles and tours`() {
        val owner = userRepository.save(User.create(displayName = "Owner", email = "owner@test.com", passwordHash = "hash"))
        val instructor = userRepository.save(User.create(displayName = "Guide", email = "guide@test.com", passwordHash = "hash"))
        val organization = organizationRepository.save(
            Organization.create(
                slug = "seoul-catalog",
                name = "Seoul Catalog",
                description = "operator profile",
                publicDescription = "public profile",
                contactEmail = "ops@seoul.test",
                contactPhone = "+82 10 1111 1111",
                websiteUrl = "https://seoul.test",
                businessName = "Seoul Catalog LLC",
                businessRegistrationNumber = "123-45-67890",
                timezone = "Asia/Seoul",
                now = Instant.parse("2026-03-18T00:00:00Z")
            )
        )
        organizationMembershipRepository.save(
            OrganizationMembership.active(
                organizationId = requireNotNull(organization.id),
                userId = requireNotNull(owner.id),
                role = OrganizationRole.OWNER,
                now = Instant.parse("2026-03-18T00:00:00Z")
            )
        )

        val registration = instructorRegistrationRepository.save(
            InstructorRegistration(
                organizationId = requireNotNull(organization.id),
                userId = requireNotNull(instructor.id),
                headline = "Storyteller",
                bio = "Guide bio",
                languages = listOf("ko", "en"),
                specialties = listOf("history"),
                status = InstructorRegistrationStatus.APPROVED,
                reviewedByUserId = requireNotNull(owner.id),
                reviewedAt = Instant.parse("2026-03-18T00:10:00Z"),
                createdAt = Instant.parse("2026-03-18T00:00:00Z"),
                updatedAt = Instant.parse("2026-03-18T00:10:00Z")
            )
        )
        val profile = instructorProfileRepository.save(
            InstructorProfile(
                organizationId = requireNotNull(organization.id),
                userId = requireNotNull(instructor.id),
                headline = "Lead storyteller",
                bio = "Public bio",
                languages = listOf("ko", "en"),
                specialties = listOf("history"),
                certifications = listOf("first aid"),
                yearsOfExperience = 6,
                internalNote = "private note",
                status = InstructorProfileStatus.ACTIVE,
                approvedAt = Instant.parse("2026-03-18T00:10:00Z"),
                createdAt = Instant.parse("2026-03-18T00:00:00Z"),
                updatedAt = Instant.parse("2026-03-18T00:10:00Z")
            )
        )
        val tour = tourRepository.save(
            Tour(
                organizationId = requireNotNull(organization.id),
                title = "Seoul Night Walk",
                summary = "Walk summary",
                status = TourStatus.PUBLISHED,
                content = TourContent(
                    description = "Tour description",
                    highlights = listOf("lantern alley"),
                    inclusions = listOf("tea"),
                    exclusions = listOf("pickup"),
                    preparations = listOf("comfortable shoes"),
                    policies = listOf("24h cancellation")
                ),
                attachmentAssetIds = listOf(501L, 502L),
                publishedAt = Instant.parse("2026-03-18T01:00:00Z"),
                createdAt = Instant.parse("2026-03-18T00:00:00Z"),
                updatedAt = Instant.parse("2026-03-18T01:00:00Z")
            )
        )

        assertNotNull(registration.id)
        assertNotNull(profile.id)
        assertNotNull(tour.id)
        assertEquals(
            listOf("ko", "en"),
            instructorRegistrationRepository.findById(requireNotNull(registration.id))?.languages
        )
        assertEquals(
            listOf("first aid"),
            instructorProfileRepository.findByOrganizationIdAndUserId(requireNotNull(organization.id), requireNotNull(instructor.id))?.certifications
        )
        assertEquals(
            listOf("lantern alley"),
            tourRepository.findById(requireNotNull(tour.id))?.content?.highlights
        )
        assertEquals(
            listOf(501L, 502L),
            tourRepository.findById(requireNotNull(tour.id))?.attachmentAssetIds
        )
    }

    @Test
    fun `mysql adapters persist assets favorites notifications and topology attachments`() {
        val user = userRepository.save(User.create(displayName = "Customer", email = "customer@test.com", passwordHash = "hash"))
        val organization = organizationRepository.save(
            Organization.create(
                slug = "asset-jpa",
                name = "Asset JPA",
                description = null,
                publicDescription = null,
                contactEmail = null,
                contactPhone = null,
                websiteUrl = null,
                businessName = null,
                businessRegistrationNumber = null,
                timezone = "Asia/Seoul",
                now = Instant.parse("2026-03-18T00:00:00Z")
            )
        )
        val asset = assetRepository.save(
            Asset(
                ownerUserId = requireNotNull(user.id),
                organizationId = requireNotNull(organization.id),
                status = AssetStatus.READY,
                fileName = "cover.jpg",
                contentType = "image/jpeg",
                storageKey = "users/1/assets/1/cover.jpg",
                uploadUrl = "https://asset.test/upload/1",
                publicUrl = "https://asset.test/public/1",
                sizeBytes = 1024,
                checksumSha256 = "a".repeat(64),
                createdAt = Instant.parse("2026-03-18T00:00:00Z"),
                completedAt = Instant.parse("2026-03-18T00:01:00Z")
            )
        )
        val updatedOrganization = organizationRepository.save(
            requireNotNull(organizationRepository.findById(requireNotNull(organization.id))).updateAttachments(
                assetIds = listOf(requireNotNull(asset.id)),
                now = Instant.parse("2026-03-18T00:02:00Z")
            )
        )
        val tour = tourRepository.save(
            Tour.create(
                organizationId = requireNotNull(organization.id),
                title = "Asset Tour",
                summary = "Summary",
                now = Instant.parse("2026-03-18T00:00:00Z")
            ).copy(status = TourStatus.PUBLISHED, attachmentAssetIds = listOf(requireNotNull(asset.id)))
        )
        favoriteRepository.save(
            Favorite(
                userId = requireNotNull(user.id),
                tourId = requireNotNull(tour.id),
                createdAt = Instant.parse("2026-03-18T00:03:00Z")
            )
        )
        notificationRepository.save(
            Notification(
                userId = requireNotNull(user.id),
                type = NotificationType.BOOKING,
                title = "Booking update",
                body = "BOOKING_CREATED for booking 1",
                resourceType = "BOOKING",
                resourceId = 1L,
                createdAt = Instant.parse("2026-03-18T00:04:00Z")
            )
        )

        assertEquals(listOf(requireNotNull(asset.id)), updatedOrganization.attachmentAssetIds)
        assertEquals(1, favoriteRepository.findByUserId(requireNotNull(user.id)).size)
        assertEquals(1, notificationRepository.findByUserId(requireNotNull(user.id)).size)
        assertEquals("https://asset.test/public/1", assetRepository.findById(requireNotNull(asset.id))?.publicUrl)
    }

    @Test
    fun `mysql adapters persist payment provider events and reconciliation summaries`() {
        paymentProviderEventRepository.save(
            PaymentProviderEvent(
                providerName = "stub-pay",
                providerEventId = "evt-jpa-1",
                eventType = PaymentProviderEventType.CAPTURED,
                bookingId = 901L,
                payloadJson = """{"ok":true}""",
                signature = "sig",
                signatureKeyId = "current",
                payloadSha256 = "hash-jpa-1",
                status = PaymentProviderEventStatus.PROCESSED,
                note = "CAPTURED",
                receivedAtUtc = Instant.parse("2026-03-18T00:00:00Z"),
                processedAtUtc = Instant.parse("2026-03-18T00:00:01Z")
            )
        )
        paymentReconciliationSummaryRepository.save(
            PaymentReconciliationDailySummary(
                summaryDate = java.time.LocalDate.parse("2026-03-18"),
                bookingCreatedCount = 3,
                authorizedCount = 1,
                capturedCount = 2,
                providerCapturedCount = 2,
                providerRefundedCount = 1,
                refundPendingCount = 0,
                refundedCount = 1,
                noRefundCount = 0,
                refundFailedRetryableCount = 1,
                refundReviewRequiredCount = 0,
                captureMismatchCount = 0,
                refundMismatchCount = 0,
                internalStatusMismatchCount = 0,
                refreshedAtUtc = Instant.parse("2026-03-18T01:00:00Z")
            )
        )

        val event = paymentProviderEventRepository.findByProviderEventId("evt-jpa-1")
        val summary = paymentReconciliationSummaryRepository.findByDate(java.time.LocalDate.parse("2026-03-18"))

        assertNotNull(event)
        assertEquals(PaymentProviderEventType.CAPTURED, event.eventType)
        assertNotNull(summary)
        assertEquals(2, summary.capturedCount)
    }

    @Test
    fun `mysql adapters persist worker job locks`() {
        val acquired = workerJobLockRepository.tryAcquire(
            lockName = "offer-expiration",
            ownerId = "mysql-test-worker",
            lockedAtUtc = Instant.parse("2026-03-18T00:00:00Z"),
            leaseExpiresAtUtc = Instant.parse("2026-03-18T00:02:00Z")
        )

        assertTrue(acquired)
        assertEquals(1, workerJobLockRepository.findAll().size)

        workerJobLockRepository.release("offer-expiration", "mysql-test-worker")

        assertTrue(workerJobLockRepository.findAll().isEmpty())
    }

    @Test
    fun `mysql adapters persist operator remediation metadata`() {
        val saved = operatorFailureRecordRepository.save(
            OperatorFailureRecord(
                sourceType = OperatorFailureSourceType.PAYMENT_WEBHOOK,
                sourceKey = "evt-operator-1",
                status = OperatorFailureRecordStatus.RESOLVED,
                lastAction = OperatorFailureAction.RESOLVE,
                note = "resolved after partner confirmation",
                lastActionByUserId = 9001L,
                lastActionAtUtc = Instant.parse("2026-03-19T12:00:00Z"),
                retryCount = 2,
                createdAtUtc = Instant.parse("2026-03-19T11:50:00Z"),
                updatedAtUtc = Instant.parse("2026-03-19T12:00:00Z")
            )
        )

        val reloaded = operatorFailureRecordRepository.findBySource(OperatorFailureSourceType.PAYMENT_WEBHOOK, "evt-operator-1")

        assertNotNull(saved.id)
        assertEquals(OperatorFailureRecordStatus.RESOLVED, reloaded?.status)
        assertEquals(2, reloaded?.retryCount)
        assertEquals(1, operatorFailureRecordRepository.findAll().size)
    }
}
