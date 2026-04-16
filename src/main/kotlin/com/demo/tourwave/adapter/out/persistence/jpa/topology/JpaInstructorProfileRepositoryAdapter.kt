package com.demo.tourwave.adapter.out.persistence.jpa.topology

import com.demo.tourwave.application.instructor.port.InstructorProfileRepository
import com.demo.tourwave.domain.instructor.InstructorProfile
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Repository

@Repository
@Profile("mysql", "mysql-test")
class JpaInstructorProfileRepositoryAdapter(
    private val instructorProfileJpaRepository: InstructorProfileJpaRepository
) : InstructorProfileRepository {
    override fun save(instructorProfile: InstructorProfile): InstructorProfile {
        return instructorProfileJpaRepository.save(instructorProfile.toEntity()).toDomain()
    }

    override fun findById(instructorProfileId: Long): InstructorProfile? =
        instructorProfileJpaRepository.findById(instructorProfileId).orElse(null)?.toDomain()

    override fun findByOrganizationId(organizationId: Long): List<InstructorProfile> =
        instructorProfileJpaRepository.findByOrganizationIdOrderByIdAsc(organizationId).map { it.toDomain() }

    override fun findByOrganizationIdAndUserId(organizationId: Long, userId: Long): InstructorProfile? =
        instructorProfileJpaRepository.findByOrganizationIdAndUserId(organizationId, userId)?.toDomain()

    override fun clear() {
        instructorProfileJpaRepository.deleteAllInBatch()
    }
}

private fun InstructorProfile.toEntity(): InstructorProfileJpaEntity =
    InstructorProfileJpaEntity(
        id = id,
        organizationId = organizationId,
        userId = userId,
        headline = headline,
        bio = bio,
        languagesJson = TopologyJsonCodec.writeList(languages),
        specialtiesJson = TopologyJsonCodec.writeList(specialties),
        certificationsJson = TopologyJsonCodec.writeList(certifications),
        yearsOfExperience = yearsOfExperience,
        internalNote = internalNote,
        status = status,
        approvedAt = approvedAt,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

private fun InstructorProfileJpaEntity.toDomain(): InstructorProfile =
    InstructorProfile(
        id = id,
        organizationId = organizationId,
        userId = userId,
        headline = headline,
        bio = bio,
        languages = TopologyJsonCodec.readList(languagesJson),
        specialties = TopologyJsonCodec.readList(specialtiesJson),
        certifications = TopologyJsonCodec.readList(certificationsJson),
        yearsOfExperience = yearsOfExperience,
        internalNote = internalNote,
        status = status,
        approvedAt = approvedAt,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
