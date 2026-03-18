package com.demo.tourwave.application.communication

import com.demo.tourwave.application.communication.port.AnnouncementRepository
import com.demo.tourwave.application.topology.OrganizationAccessGuard
import com.demo.tourwave.domain.announcement.Announcement
import com.demo.tourwave.domain.announcement.AnnouncementVisibility
import com.demo.tourwave.domain.common.DomainException
import com.demo.tourwave.domain.common.ErrorCode
import java.time.Clock

data class CreateAnnouncementCommand(
    val actorUserId: Long,
    val organizationId: Long,
    val title: String,
    val body: String,
    val visibility: AnnouncementVisibility,
    val publishStartsAtUtc: java.time.Instant?,
    val publishEndsAtUtc: java.time.Instant?
)

data class UpdateAnnouncementCommand(
    val actorUserId: Long,
    val announcementId: Long,
    val title: String?,
    val body: String?,
    val visibility: AnnouncementVisibility?,
    val publishStartsAtUtc: java.time.Instant?,
    val publishEndsAtUtc: java.time.Instant?
)

data class AnnouncementCursorPage(
    val items: List<Announcement>,
    val nextCursor: String?
)

class AnnouncementService(
    private val announcementRepository: AnnouncementRepository,
    private val organizationAccessGuard: OrganizationAccessGuard,
    private val clock: Clock
) {
    fun create(command: CreateAnnouncementCommand): Announcement {
        organizationAccessGuard.requireOperator(command.actorUserId, command.organizationId)
        return announcementRepository.save(
            Announcement.create(
                organizationId = command.organizationId,
                title = command.title.trim(),
                body = command.body.trim(),
                visibility = command.visibility,
                publishStartsAtUtc = command.publishStartsAtUtc,
                publishEndsAtUtc = command.publishEndsAtUtc,
                now = clock.instant()
            )
        )
    }

    fun listOperatorAnnouncements(
        actorUserId: Long,
        organizationId: Long,
        cursor: String?,
        limit: Int
    ): AnnouncementCursorPage {
        organizationAccessGuard.requireOperator(actorUserId, organizationId)
        return paginate(
            announcements = announcementRepository.findByOrganizationId(organizationId),
            cursor = cursor,
            limit = limit
        )
    }

    fun listPublicAnnouncements(
        organizationId: Long?,
        cursor: String?,
        limit: Int
    ): AnnouncementCursorPage {
        val now = clock.instant()
        val announcements = (organizationId?.let(announcementRepository::findByOrganizationId)
            ?: announcementRepository.findAll())
            .filter { it.isVisibleToPublic(now) }
        return paginate(announcements, cursor, limit)
    }

    fun update(command: UpdateAnnouncementCommand): Announcement {
        val existing = requireAnnouncement(command.announcementId)
        organizationAccessGuard.requireOperator(command.actorUserId, existing.organizationId)
        return announcementRepository.save(
            existing.update(
                title = command.title?.trim() ?: existing.title,
                body = command.body?.trim() ?: existing.body,
                visibility = command.visibility ?: existing.visibility,
                publishStartsAtUtc = command.publishStartsAtUtc ?: existing.publishStartsAtUtc,
                publishEndsAtUtc = command.publishEndsAtUtc ?: existing.publishEndsAtUtc,
                now = clock.instant()
            )
        )
    }

    fun delete(actorUserId: Long, announcementId: Long) {
        val existing = requireAnnouncement(announcementId)
        organizationAccessGuard.requireOperator(actorUserId, existing.organizationId)
        announcementRepository.deleteById(announcementId)
    }

    private fun requireAnnouncement(announcementId: Long): Announcement {
        return announcementRepository.findById(announcementId) ?: throw DomainException(
            errorCode = ErrorCode.VALIDATION_ERROR,
            status = 404,
            message = "announcement $announcementId not found"
        )
    }

    private fun paginate(
        announcements: List<Announcement>,
        cursor: String?,
        limit: Int
    ): AnnouncementCursorPage {
        val safeLimit = limit.coerceIn(1, 100)
        val sorted = announcements.sortedWith(compareByDescending<Announcement> { it.updatedAt }.thenByDescending { it.id ?: -1L })
        val filtered = cursor?.toLongOrNull()?.let { cursorId ->
            sorted.dropWhile { (it.id ?: Long.MIN_VALUE) != cursorId }.drop(1)
        } ?: sorted
        val items = filtered.take(safeLimit)
        val nextCursor = items.takeIf { filtered.size > safeLimit }?.lastOrNull()?.id?.toString()
        return AnnouncementCursorPage(items = items, nextCursor = nextCursor)
    }
}
