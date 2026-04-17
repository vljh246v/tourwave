package com.demo.tourwave.application.asset

import com.demo.tourwave.adapter.out.persistence.asset.InMemoryAssetRepositoryAdapter
import com.demo.tourwave.application.asset.port.AssetStoragePort
import com.demo.tourwave.application.asset.port.AssetUploadDescriptor
import com.demo.tourwave.application.asset.port.AssetUploadVerificationRequest
import com.demo.tourwave.application.asset.port.StoredAssetMetadata
import com.demo.tourwave.application.organization.OrganizationAccessGuard
import com.demo.tourwave.domain.asset.AssetStatus
import com.demo.tourwave.domain.organization.Organization
import com.demo.tourwave.domain.organization.OrganizationMembership
import com.demo.tourwave.domain.organization.OrganizationRole
import com.demo.tourwave.domain.tour.Tour
import com.demo.tourwave.domain.user.User
import com.demo.tourwave.support.FakeOrganizationMembershipRepository
import com.demo.tourwave.support.FakeOrganizationRepository
import com.demo.tourwave.support.FakeTourRepository
import com.demo.tourwave.support.FakeUserRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AssetCommandServiceTest {
    private val clock = Clock.fixed(Instant.parse("2026-03-18T00:00:00Z"), ZoneOffset.UTC)
    private val assetRepository = InMemoryAssetRepositoryAdapter()
    private val organizationRepository = FakeOrganizationRepository()
    private val membershipRepository = FakeOrganizationMembershipRepository()
    private val tourRepository = FakeTourRepository()
    private val userRepository = FakeUserRepository()
    private val storagePort =
        object : AssetStoragePort {
            override fun issueUpload(
                ownerUserId: Long,
                assetIdHint: Long?,
                fileName: String,
                contentType: String,
            ): AssetUploadDescriptor {
                val id = assetIdHint ?: 999L
                return AssetUploadDescriptor(
                    storageKey = "users/$ownerUserId/assets/$id/$fileName",
                    uploadUrl = "https://asset.test/upload/$id",
                    publicUrl = "https://asset.test/public/$id",
                )
            }

            override fun verifyUpload(request: AssetUploadVerificationRequest): StoredAssetMetadata =
                StoredAssetMetadata(
                    publicUrl = "https://asset.test/public/999",
                    contentType = request.expectedContentType,
                    sizeBytes = request.reportedSizeBytes ?: 2048L,
                    checksumSha256 = request.reportedChecksumSha256,
                )
        }
    private val service =
        AssetCommandService(
            assetRepository = assetRepository,
            assetStoragePort = storagePort,
            organizationRepository = organizationRepository,
            tourRepository = tourRepository,
            organizationAccessGuard = OrganizationAccessGuard(organizationRepository, membershipRepository),
            userRepository = userRepository,
            clock = clock,
        )

    @BeforeEach
    fun setUp() {
        assetRepository.clear()
        organizationRepository.clear()
        membershipRepository.clear()
        tourRepository.clear()
        userRepository.clear()

        val owner =
            userRepository.save(
                User.create(displayName = "Owner", email = "owner@test.com", passwordHash = "hash", now = clock.instant()),
            )
        val organization =
            organizationRepository.save(
                Organization.create(
                    slug = "asset-org",
                    name = "Asset Org",
                    description = null,
                    publicDescription = null,
                    contactEmail = null,
                    contactPhone = null,
                    websiteUrl = null,
                    businessName = null,
                    businessRegistrationNumber = null,
                    timezone = "Asia/Seoul",
                    now = clock.instant(),
                ),
            )
        membershipRepository.save(
            OrganizationMembership.active(
                organizationId = requireNotNull(organization.id),
                userId = requireNotNull(owner.id),
                role = OrganizationRole.OWNER,
                now = clock.instant(),
            ),
        )
        tourRepository.save(
            Tour
                .create(
                    organizationId = requireNotNull(organization.id),
                    title = "Tour",
                    summary = "Summary",
                    now = clock.instant(),
                ).copy(id = 200L),
        )
    }

    @Test
    fun `issue complete and attach asset to organization and tour`() {
        val upload =
            service.issueUpload(
                IssueAssetUploadCommand(
                    actorUserId = 1L,
                    organizationId = 1L,
                    fileName = "cover.jpg",
                    contentType = "image/jpeg",
                ),
            )

        assertEquals(AssetStatus.UPLOADING, upload.status)

        val completed =
            service.completeUpload(
                CompleteAssetUploadCommand(
                    actorUserId = 1L,
                    assetId = requireNotNull(upload.id),
                    sizeBytes = 2048,
                    checksumSha256 = "a".repeat(64),
                ),
            )

        assertEquals(AssetStatus.READY, completed.status)
        assertEquals("https://asset.test/public/999", completed.publicUrl)

        val organizationAssetIds =
            service.attachOrganizationAssets(
                AttachOrganizationAssetsCommand(
                    actorUserId = 1L,
                    organizationId = 1L,
                    assetIds = listOf(requireNotNull(completed.id)),
                ),
            )
        val tourAssetIds =
            service.attachTourAssets(
                AttachTourAssetsCommand(
                    actorUserId = 1L,
                    tourId = 200L,
                    assetIds = listOf(requireNotNull(completed.id)),
                ),
            )

        assertEquals(listOf(requireNotNull(completed.id)), organizationAssetIds)
        assertEquals(listOf(requireNotNull(completed.id)), tourAssetIds)
        assertEquals(listOf(requireNotNull(completed.id)), organizationRepository.findById(1L)?.attachmentAssetIds)
        assertEquals(listOf(requireNotNull(completed.id)), tourRepository.findById(200L)?.attachmentAssetIds)
    }

    @Test
    fun `complete upload rejects metadata mismatch`() {
        val upload =
            service.issueUpload(
                IssueAssetUploadCommand(
                    actorUserId = 1L,
                    organizationId = 1L,
                    fileName = "cover.jpg",
                    contentType = "image/jpeg",
                ),
            )

        val failingService =
            AssetCommandService(
                assetRepository = assetRepository,
                assetStoragePort =
                    object : AssetStoragePort {
                        override fun issueUpload(
                            ownerUserId: Long,
                            assetIdHint: Long?,
                            fileName: String,
                            contentType: String,
                        ): AssetUploadDescriptor = storagePort.issueUpload(ownerUserId, assetIdHint, fileName, contentType)

                        override fun verifyUpload(request: AssetUploadVerificationRequest): StoredAssetMetadata =
                            StoredAssetMetadata(
                                publicUrl = "https://asset.test/public/999",
                                contentType = "image/png",
                                sizeBytes = 1024L,
                                checksumSha256 = "b".repeat(64),
                            )
                    },
                organizationRepository = organizationRepository,
                tourRepository = tourRepository,
                organizationAccessGuard = OrganizationAccessGuard(organizationRepository, membershipRepository),
                userRepository = userRepository,
                clock = clock,
            )

        assertFailsWith<com.demo.tourwave.domain.common.DomainException> {
            failingService.completeUpload(
                CompleteAssetUploadCommand(
                    actorUserId = 1L,
                    assetId = requireNotNull(upload.id),
                    sizeBytes = 2048L,
                    checksumSha256 = "a".repeat(64),
                ),
            )
        }
    }
}
