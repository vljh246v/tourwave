package com.demo.tourwave.adapter.out.persistence.jpa.tour

import com.demo.tourwave.adapter.out.persistence.jpa.JpaJsonCodec
import com.demo.tourwave.application.tour.port.TourRepository
import com.demo.tourwave.domain.tour.Tour
import com.demo.tourwave.domain.tour.TourContent
import com.demo.tourwave.domain.tour.TourStatus
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Repository

@Repository
@Profile("mysql", "mysql-test")
class JpaTourRepositoryAdapter(
    private val tourJpaRepository: TourJpaRepository,
) : TourRepository {
    override fun save(tour: Tour): Tour {
        return tourJpaRepository.save(tour.toEntity()).toDomain()
    }

    override fun findById(tourId: Long): Tour? = tourJpaRepository.findById(tourId).orElse(null)?.toDomain()

    override fun findByOrganizationId(organizationId: Long): List<Tour> =
        tourJpaRepository.findByOrganizationIdOrderByIdAsc(organizationId).map { it.toDomain() }

    override fun findAllPublished(): List<Tour> = tourJpaRepository.findByStatusOrderByIdAsc(TourStatus.PUBLISHED).map { it.toDomain() }

    override fun clear() {
        tourJpaRepository.deleteAllInBatch()
    }
}

private fun Tour.toEntity(): TourJpaEntity =
    TourJpaEntity(
        id = id,
        organizationId = organizationId,
        title = title,
        summary = summary,
        status = status,
        description = content.description,
        highlightsJson = JpaJsonCodec.writeList(content.highlights),
        inclusionsJson = JpaJsonCodec.writeList(content.inclusions),
        exclusionsJson = JpaJsonCodec.writeList(content.exclusions),
        preparationsJson = JpaJsonCodec.writeList(content.preparations),
        policiesJson = JpaJsonCodec.writeList(content.policies),
        attachmentAssetIdsJson = JpaJsonCodec.writeLongList(attachmentAssetIds),
        publishedAt = publishedAt,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

private fun TourJpaEntity.toDomain(): Tour =
    Tour(
        id = id,
        organizationId = organizationId,
        title = title,
        summary = summary,
        status = status,
        content =
            TourContent(
                description = description,
                highlights = JpaJsonCodec.readList(highlightsJson),
                inclusions = JpaJsonCodec.readList(inclusionsJson),
                exclusions = JpaJsonCodec.readList(exclusionsJson),
                preparations = JpaJsonCodec.readList(preparationsJson),
                policies = JpaJsonCodec.readList(policiesJson),
            ),
        attachmentAssetIds = JpaJsonCodec.readLongList(attachmentAssetIdsJson),
        publishedAt = publishedAt,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
