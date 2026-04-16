package com.demo.tourwave.adapter.out.persistence.jpa.instructor

import com.demo.tourwave.adapter.out.persistence.jpa.JpaJsonCodec
import com.demo.tourwave.application.instructor.port.InstructorRegistrationRepository
import com.demo.tourwave.domain.instructor.InstructorRegistration
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Repository

@Repository
@Profile("mysql", "mysql-test")
class JpaInstructorRegistrationRepositoryAdapter(
    private val instructorRegistrationJpaRepository: InstructorRegistrationJpaRepository,
) : InstructorRegistrationRepository {
    override fun save(registration: InstructorRegistration): InstructorRegistration {
        return instructorRegistrationJpaRepository.save(registration.toEntity()).toDomain()
    }

    override fun findById(registrationId: Long): InstructorRegistration? =
        instructorRegistrationJpaRepository.findById(registrationId).orElse(null)?.toDomain()

    override fun findByOrganizationId(organizationId: Long): List<InstructorRegistration> =
        instructorRegistrationJpaRepository.findByOrganizationIdOrderByIdAsc(organizationId).map { it.toDomain() }

    override fun findByOrganizationIdAndUserId(
        organizationId: Long,
        userId: Long,
    ): InstructorRegistration? = instructorRegistrationJpaRepository.findByOrganizationIdAndUserId(organizationId, userId)?.toDomain()

    override fun clear() {
        instructorRegistrationJpaRepository.deleteAllInBatch()
    }
}

private fun InstructorRegistration.toEntity(): InstructorRegistrationJpaEntity =
    InstructorRegistrationJpaEntity(
        id = id,
        organizationId = organizationId,
        userId = userId,
        headline = headline,
        bio = bio,
        languagesJson = JpaJsonCodec.writeList(languages),
        specialtiesJson = JpaJsonCodec.writeList(specialties),
        status = status,
        rejectionReason = rejectionReason,
        reviewedByUserId = reviewedByUserId,
        reviewedAt = reviewedAt,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

private fun InstructorRegistrationJpaEntity.toDomain(): InstructorRegistration =
    InstructorRegistration(
        id = id,
        organizationId = organizationId,
        userId = userId,
        headline = headline,
        bio = bio,
        languages = JpaJsonCodec.readList(languagesJson),
        specialties = JpaJsonCodec.readList(specialtiesJson),
        status = status,
        rejectionReason = rejectionReason,
        reviewedByUserId = reviewedByUserId,
        reviewedAt = reviewedAt,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
