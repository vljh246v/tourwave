package com.demo.tourwave.adapter.out.persistence.jpa.announcement

import com.demo.tourwave.application.announcement.port.AnnouncementRepository
import com.demo.tourwave.domain.announcement.Announcement
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Repository

@Repository
@Profile("mysql", "mysql-test")
class JpaAnnouncementRepositoryAdapter(
    private val announcementJpaRepository: AnnouncementJpaRepository
) : AnnouncementRepository {
    override fun save(announcement: Announcement): Announcement =
        announcementJpaRepository.save(announcement.toEntity()).toDomain()

    override fun findById(announcementId: Long): Announcement? =
        announcementJpaRepository.findById(announcementId).orElse(null)?.toDomain()

    override fun findByOrganizationId(organizationId: Long): List<Announcement> =
        announcementJpaRepository.findByOrganizationIdOrderByUpdatedAtDescIdDesc(organizationId).map { it.toDomain() }

    override fun findAll(): List<Announcement> =
        announcementJpaRepository.findAll().map { it.toDomain() }.sortedWith(compareByDescending<Announcement> { it.updatedAt }.thenByDescending { it.id ?: -1L })

    override fun deleteById(announcementId: Long) {
        announcementJpaRepository.deleteById(announcementId)
    }

    override fun clear() {
        announcementJpaRepository.deleteAllInBatch()
    }
}

private fun Announcement.toEntity(): AnnouncementJpaEntity =
    AnnouncementJpaEntity(
        id = id,
        organizationId = organizationId,
        title = title,
        body = body,
        visibility = visibility,
        publishStartsAtUtc = publishStartsAtUtc,
        publishEndsAtUtc = publishEndsAtUtc,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

private fun AnnouncementJpaEntity.toDomain(): Announcement =
    Announcement(
        id = id,
        organizationId = organizationId,
        title = title,
        body = body,
        visibility = visibility,
        publishStartsAtUtc = publishStartsAtUtc,
        publishEndsAtUtc = publishEndsAtUtc,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
