package com.demo.tourwave.adapter.out.persistence.jpa.topology

import com.demo.tourwave.application.tour.port.TourRepository
import com.demo.tourwave.domain.tour.Tour
import com.demo.tourwave.domain.tour.TourContent
import com.demo.tourwave.domain.tour.TourStatus
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Repository

@Repository
@Profile("mysql", "mysql-test")
class JpaTourRepositoryAdapter(
    private val tourJpaRepository: TourJpaRepository
) : TourRepository {
    override fun save(tour: Tour): Tour {
        return tourJpaRepository.save(tour.toEntity()).toDomain()
    }

    override fun findById(tourId: Long): Tour? =
        tourJpaRepository.findById(tourId).orElse(null)?.toDomain()

    override fun findByOrganizationId(organizationId: Long): List<Tour> =
        tourJpaRepository.findByOrganizationIdOrderByIdAsc(organizationId).map { it.toDomain() }

    override fun findAllPublished(): List<Tour> =
        tourJpaRepository.findByStatusOrderByIdAsc(TourStatus.PUBLISHED).map { it.toDomain() }

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
        highlightsJson = TopologyJsonCodec.writeList(content.highlights),
        inclusionsJson = TopologyJsonCodec.writeList(content.inclusions),
        exclusionsJson = TopologyJsonCodec.writeList(content.exclusions),
        preparationsJson = TopologyJsonCodec.writeList(content.preparations),
        policiesJson = TopologyJsonCodec.writeList(content.policies),
        attachmentAssetIdsJson = TopologyJsonCodec.writeLongList(attachmentAssetIds),
        publishedAt = publishedAt,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

private fun TourJpaEntity.toDomain(): Tour =
    Tour(
        id = id,
        organizationId = organizationId,
        title = title,
        summary = summary,
        status = status,
        content = TourContent(
            description = description,
            highlights = TopologyJsonCodec.readList(highlightsJson),
            inclusions = TopologyJsonCodec.readList(inclusionsJson),
            exclusions = TopologyJsonCodec.readList(exclusionsJson),
            preparations = TopologyJsonCodec.readList(preparationsJson),
            policies = TopologyJsonCodec.readList(policiesJson)
        ),
        attachmentAssetIds = TopologyJsonCodec.readLongList(attachmentAssetIdsJson),
        publishedAt = publishedAt,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
