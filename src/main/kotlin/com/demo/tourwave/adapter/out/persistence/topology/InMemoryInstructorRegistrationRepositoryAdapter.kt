package com.demo.tourwave.adapter.out.persistence.topology

import com.demo.tourwave.application.topology.port.InstructorRegistrationRepository
import com.demo.tourwave.domain.instructor.InstructorRegistration
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Repository
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

@Repository
@Profile("!mysql & !mysql-test")
class InMemoryInstructorRegistrationRepositoryAdapter : InstructorRegistrationRepository {
    private val sequence = AtomicLong(0L)
    private val registrations = ConcurrentHashMap<Long, InstructorRegistration>()
    private val registrationIdsByOrgUser = ConcurrentHashMap<String, Long>()

    override fun save(registration: InstructorRegistration): InstructorRegistration {
        val registrationId = registration.id ?: registrationIdsByOrgUser[keyOf(registration.organizationId, registration.userId)] ?: sequence.incrementAndGet()
        val saved = registration.copy(id = registrationId)
        registrations[registrationId] = saved
        registrationIdsByOrgUser[keyOf(saved.organizationId, saved.userId)] = registrationId
        return saved
    }

    override fun findById(registrationId: Long): InstructorRegistration? = registrations[registrationId]

    override fun findByOrganizationId(organizationId: Long): List<InstructorRegistration> =
        registrations.values.filter { it.organizationId == organizationId }.sortedBy { it.id }

    override fun findByOrganizationIdAndUserId(organizationId: Long, userId: Long): InstructorRegistration? =
        registrationIdsByOrgUser[keyOf(organizationId, userId)]?.let(registrations::get)

    override fun clear() {
        sequence.set(0L)
        registrations.clear()
        registrationIdsByOrgUser.clear()
    }

    private fun keyOf(organizationId: Long, userId: Long) = "$organizationId:$userId"
}
