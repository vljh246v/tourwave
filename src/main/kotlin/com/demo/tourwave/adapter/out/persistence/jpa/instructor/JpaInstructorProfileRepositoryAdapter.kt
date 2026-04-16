package com.demo.tourwave.adapter.out.persistence.jpa.instructor

import com.demo.tourwave.adapter.out.persistence.jpa.JpaJsonCodec
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
        languagesJson = JpaJsonCodec.writeList(languages),
        specialtiesJson = JpaJsonCodec.writeList(specialties),
        certificationsJson = JpaJsonCodec.writeList(certifications),
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
        languages = JpaJsonCodec.readList(languagesJson),
        specialties = JpaJsonCodec.readList(specialtiesJson),
        certifications = JpaJsonCodec.readList(certificationsJson),
        yearsOfExperience = yearsOfExperience,
        internalNote = internalNote,
        status = status,
        approvedAt = approvedAt,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
