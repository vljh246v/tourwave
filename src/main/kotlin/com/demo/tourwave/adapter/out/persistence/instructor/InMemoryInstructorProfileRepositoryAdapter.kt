package com.demo.tourwave.adapter.out.persistence.instructor

import com.demo.tourwave.application.instructor.port.InstructorProfileRepository
import com.demo.tourwave.domain.instructor.InstructorProfile
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Repository
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

@Repository
@Profile("!mysql & !mysql-test")
class InMemoryInstructorProfileRepositoryAdapter : InstructorProfileRepository {
    private val sequence = AtomicLong(0L)
    private val instructorProfiles = ConcurrentHashMap<Long, InstructorProfile>()
    private val profileIdsByOrgUser = ConcurrentHashMap<String, Long>()

    override fun save(instructorProfile: InstructorProfile): InstructorProfile {
        val instructorProfileId =
            instructorProfile.id
                ?: profileIdsByOrgUser[keyOf(instructorProfile.organizationId, instructorProfile.userId)]
                ?: sequence.incrementAndGet()
        val saved = instructorProfile.copy(id = instructorProfileId)
        instructorProfiles[instructorProfileId] = saved
        profileIdsByOrgUser[keyOf(saved.organizationId, saved.userId)] = instructorProfileId
        return saved
    }

    override fun findById(instructorProfileId: Long): InstructorProfile? = instructorProfiles[instructorProfileId]

    override fun findByOrganizationId(organizationId: Long): List<InstructorProfile> {
        return instructorProfiles.values.filter { it.organizationId == organizationId }.sortedBy { it.id }
    }

    override fun findByOrganizationIdAndUserId(
        organizationId: Long,
        userId: Long,
    ): InstructorProfile? {
        return profileIdsByOrgUser[keyOf(organizationId, userId)]?.let(instructorProfiles::get)
    }

    override fun clear() {
        sequence.set(0L)
        instructorProfiles.clear()
        profileIdsByOrgUser.clear()
    }

    private fun keyOf(
        organizationId: Long,
        userId: Long,
    ) = "$organizationId:$userId"
}
