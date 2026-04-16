package com.demo.tourwave.application.announcement.port

import com.demo.tourwave.domain.announcement.Announcement

interface AnnouncementRepository {
    fun save(announcement: Announcement): Announcement
    fun findById(announcementId: Long): Announcement?
    fun findByOrganizationId(organizationId: Long): List<Announcement>
    fun findAll(): List<Announcement>
    fun deleteById(announcementId: Long)
    fun clear()
}
