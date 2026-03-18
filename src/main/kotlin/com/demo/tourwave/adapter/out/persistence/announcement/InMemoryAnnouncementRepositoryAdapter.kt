package com.demo.tourwave.adapter.out.persistence.announcement

import com.demo.tourwave.application.communication.port.AnnouncementRepository
import com.demo.tourwave.domain.announcement.Announcement
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Repository
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

@Repository
@Profile("!mysql & !mysql-test")
class InMemoryAnnouncementRepositoryAdapter : AnnouncementRepository {
    private val sequence = AtomicLong(0L)
    private val announcements = ConcurrentHashMap<Long, Announcement>()

    override fun save(announcement: Announcement): Announcement {
        val id = announcement.id ?: sequence.incrementAndGet()
        val saved = announcement.copy(id = id)
        announcements[id] = saved
        return saved
    }

    override fun findById(announcementId: Long): Announcement? = announcements[announcementId]

    override fun findByOrganizationId(organizationId: Long): List<Announcement> =
        announcements.values.filter { it.organizationId == organizationId }.sortedBy { it.id }

    override fun findAll(): List<Announcement> = announcements.values.sortedBy { it.id }

    override fun deleteById(announcementId: Long) {
        announcements.remove(announcementId)
    }

    override fun clear() {
        announcements.clear()
        sequence.set(0L)
    }
}
