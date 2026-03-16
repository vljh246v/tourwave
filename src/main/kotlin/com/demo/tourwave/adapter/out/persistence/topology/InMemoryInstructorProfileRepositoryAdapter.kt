package com.demo.tourwave.adapter.out.persistence.topology

import com.demo.tourwave.application.topology.port.InstructorProfileRepository
import com.demo.tourwave.domain.instructor.InstructorProfile
import org.springframework.stereotype.Repository
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

@Repository
class InMemoryInstructorProfileRepositoryAdapter : InstructorProfileRepository {
    private val sequence = AtomicLong(0L)
    private val instructorProfiles = ConcurrentHashMap<Long, InstructorProfile>()

    override fun save(instructorProfile: InstructorProfile): InstructorProfile {
        val instructorProfileId = instructorProfile.id ?: sequence.incrementAndGet()
        val saved = instructorProfile.copy(id = instructorProfileId)
        instructorProfiles[instructorProfileId] = saved
        return saved
    }

    override fun findById(instructorProfileId: Long): InstructorProfile? = instructorProfiles[instructorProfileId]

    override fun clear() {
        sequence.set(0L)
        instructorProfiles.clear()
    }
}
