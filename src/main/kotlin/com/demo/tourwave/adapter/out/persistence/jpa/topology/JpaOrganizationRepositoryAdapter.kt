package com.demo.tourwave.adapter.out.persistence.jpa.topology

import com.demo.tourwave.application.topology.port.OrganizationRepository
import com.demo.tourwave.domain.organization.Organization
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Repository

@Repository
@Profile("mysql", "mysql-test")
class JpaOrganizationRepositoryAdapter(
    private val organizationJpaRepository: OrganizationJpaRepository
) : OrganizationRepository {
    override fun save(organization: Organization): Organization {
        return organizationJpaRepository.save(organization.toEntity()).toDomain()
    }

    override fun findById(organizationId: Long): Organization? {
        return organizationJpaRepository.findById(organizationId).orElse(null)?.toDomain()
    }

    override fun findBySlug(slug: String): Organization? = organizationJpaRepository.findBySlug(slug)?.toDomain()

    override fun findAll(): List<Organization> = organizationJpaRepository.findAll().map { it.toDomain() }

    override fun clear() {
        organizationJpaRepository.deleteAllInBatch()
    }
}

private fun Organization.toEntity(): OrganizationJpaEntity =
    OrganizationJpaEntity(
        id = id,
        slug = slug,
        name = name,
        description = description,
        publicDescription = publicDescription,
        contactEmail = contactEmail,
        contactPhone = contactPhone,
        websiteUrl = websiteUrl,
        businessName = businessName,
        businessRegistrationNumber = businessRegistrationNumber,
        attachmentAssetIdsJson = TopologyJsonCodec.writeLongList(attachmentAssetIds),
        timezone = timezone,
        status = status,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

private fun OrganizationJpaEntity.toDomain(): Organization =
    Organization(
        id = id,
        slug = slug,
        name = name,
        description = description,
        publicDescription = publicDescription,
        contactEmail = contactEmail,
        contactPhone = contactPhone,
        websiteUrl = websiteUrl,
        businessName = businessName,
        businessRegistrationNumber = businessRegistrationNumber,
        attachmentAssetIds = TopologyJsonCodec.readLongList(attachmentAssetIdsJson),
        timezone = timezone,
        status = status,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
