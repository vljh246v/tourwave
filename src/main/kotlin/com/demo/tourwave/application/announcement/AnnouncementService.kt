package com.demo.tourwave.application.announcement

import com.demo.tourwave.application.announcement.port.AnnouncementRepository
import com.demo.tourwave.application.common.port.AuditEventCommand
import com.demo.tourwave.application.common.port.AuditEventPort
import com.demo.tourwave.application.common.port.IdempotencyDecision
import com.demo.tourwave.application.common.port.IdempotencyStore
import com.demo.tourwave.application.organization.OrganizationAccessGuard
import com.demo.tourwave.domain.announcement.Announcement
import com.demo.tourwave.domain.announcement.AnnouncementVisibility
import com.demo.tourwave.domain.common.DomainException
import com.demo.tourwave.domain.common.ErrorCode
import org.springframework.transaction.annotation.Transactional
import java.security.MessageDigest
import java.time.Clock

data class CreateAnnouncementCommand(
    val actorUserId: Long,
    val organizationId: Long,
    val title: String,
    val body: String,
    val visibility: AnnouncementVisibility,
    val publishStartsAtUtc: java.time.Instant?,
    val publishEndsAtUtc: java.time.Instant?,
    val idempotencyKey: String,
)

data class UpdateAnnouncementCommand(
    val actorUserId: Long,
    val announcementId: Long,
    val title: String?,
    val body: String?,
    val visibility: AnnouncementVisibility?,
    val publishStartsAtUtc: java.time.Instant?,
    val publishEndsAtUtc: java.time.Instant?,
    val idempotencyKey: String,
)

data class DeleteAnnouncementCommand(
    val actorUserId: Long,
    val announcementId: Long,
    val idempotencyKey: String,
)

data class AnnouncementCursorPage(
    val items: List<Announcement>,
    val nextCursor: String?,
)

@Transactional
class AnnouncementService(
    private val announcementRepository: AnnouncementRepository,
    private val organizationAccessGuard: OrganizationAccessGuard,
    private val auditEventPort: AuditEventPort,
    private val idempotencyStore: IdempotencyStore,
    private val clock: Clock,
) {
    fun create(command: CreateAnnouncementCommand): Announcement {
        organizationAccessGuard.requireOperator(command.actorUserId, command.organizationId)

        val pathTemplate = "/organizations/{organizationId}/announcements"
        val requestHash =
            requestHash(
                "${command.organizationId}|${command.title}|${command.body}|${command.visibility}|${command.publishStartsAtUtc}|${command.publishEndsAtUtc}",
            )

        return when (
            val decision =
                idempotencyStore.reserveOrReplay(
                    actorUserId = command.actorUserId,
                    method = "POST",
                    pathTemplate = pathTemplate,
                    idempotencyKey = command.idempotencyKey,
                    requestHash = requestHash,
                )
        ) {
            is IdempotencyDecision.Replay -> decision.body as Announcement
            IdempotencyDecision.Reserved -> {
                val saved =
                    announcementRepository.save(
                        Announcement.create(
                            organizationId = command.organizationId,
                            title = command.title.trim(),
                            body = command.body.trim(),
                            visibility = command.visibility,
                            publishStartsAtUtc = command.publishStartsAtUtc,
                            publishEndsAtUtc = command.publishEndsAtUtc,
                            now = clock.instant(),
                        ),
                    )
                auditEventPort.append(
                    AuditEventCommand(
                        actor = "OPERATOR:${command.actorUserId}",
                        action = "ANNOUNCEMENT_CREATED",
                        resourceType = "ANNOUNCEMENT",
                        resourceId = requireNotNull(saved.id),
                        occurredAtUtc = clock.instant(),
                        reasonCode = "ANNOUNCEMENT_CREATED",
                        afterJson = announcementSnapshot(saved),
                    ),
                )
                idempotencyStore.complete(
                    actorUserId = command.actorUserId,
                    method = "POST",
                    pathTemplate = pathTemplate,
                    idempotencyKey = command.idempotencyKey,
                    status = 201,
                    body = saved,
                )
                saved
            }
        }
    }

    fun listOperatorAnnouncements(
        actorUserId: Long,
        organizationId: Long,
        cursor: String?,
        limit: Int,
    ): AnnouncementCursorPage {
        organizationAccessGuard.requireOperator(actorUserId, organizationId)
        return paginate(
            announcements = announcementRepository.findByOrganizationId(organizationId),
            cursor = cursor,
            limit = limit,
        )
    }

    fun listPublicAnnouncements(
        organizationId: Long?,
        cursor: String?,
        limit: Int,
    ): AnnouncementCursorPage {
        val now = clock.instant()
        val announcements =
            (
                organizationId?.let(announcementRepository::findByOrganizationId)
                    ?: announcementRepository.findAll()
            )
                .filter { it.isVisibleToPublic(now) }
        return paginate(announcements, cursor, limit)
    }

    fun update(command: UpdateAnnouncementCommand): Announcement {
        val pathTemplate = "/announcements/{announcementId}"
        val requestHash =
            requestHash(
                "${command.announcementId}|${command.title}|${command.body}|${command.visibility}|${command.publishStartsAtUtc}|${command.publishEndsAtUtc}",
            )

        return when (
            val decision =
                idempotencyStore.reserveOrReplay(
                    actorUserId = command.actorUserId,
                    method = "PATCH",
                    pathTemplate = pathTemplate,
                    idempotencyKey = command.idempotencyKey,
                    requestHash = requestHash,
                )
        ) {
            is IdempotencyDecision.Replay -> decision.body as Announcement
            IdempotencyDecision.Reserved -> {
                val existing = requireAnnouncement(command.announcementId)
                organizationAccessGuard.requireOperator(command.actorUserId, existing.organizationId)
                val saved =
                    announcementRepository.save(
                        existing.update(
                            title = command.title?.trim() ?: existing.title,
                            body = command.body?.trim() ?: existing.body,
                            visibility = command.visibility ?: existing.visibility,
                            publishStartsAtUtc = command.publishStartsAtUtc ?: existing.publishStartsAtUtc,
                            publishEndsAtUtc = command.publishEndsAtUtc ?: existing.publishEndsAtUtc,
                            now = clock.instant(),
                        ),
                    )
                auditEventPort.append(
                    AuditEventCommand(
                        actor = "OPERATOR:${command.actorUserId}",
                        action = "ANNOUNCEMENT_UPDATED",
                        resourceType = "ANNOUNCEMENT",
                        resourceId = requireNotNull(saved.id),
                        occurredAtUtc = clock.instant(),
                        reasonCode = "ANNOUNCEMENT_UPDATED",
                        beforeJson = announcementSnapshot(existing),
                        afterJson = announcementSnapshot(saved),
                    ),
                )
                idempotencyStore.complete(
                    actorUserId = command.actorUserId,
                    method = "PATCH",
                    pathTemplate = pathTemplate,
                    idempotencyKey = command.idempotencyKey,
                    status = 200,
                    body = saved,
                )
                saved
            }
        }
    }

    fun delete(command: DeleteAnnouncementCommand) {
        val pathTemplate = "/announcements/{announcementId}"
        val requestHash = requestHash("${command.announcementId}")

        when (
            idempotencyStore.reserveOrReplay(
                actorUserId = command.actorUserId,
                method = "DELETE",
                pathTemplate = pathTemplate,
                idempotencyKey = command.idempotencyKey,
                requestHash = requestHash,
            )
        ) {
            is IdempotencyDecision.Replay -> Unit
            IdempotencyDecision.Reserved -> {
                val existing = requireAnnouncement(command.announcementId)
                organizationAccessGuard.requireOperator(command.actorUserId, existing.organizationId)
                announcementRepository.deleteById(command.announcementId)
                auditEventPort.append(
                    AuditEventCommand(
                        actor = "OPERATOR:${command.actorUserId}",
                        action = "ANNOUNCEMENT_DELETED",
                        resourceType = "ANNOUNCEMENT",
                        resourceId = requireNotNull(existing.id),
                        occurredAtUtc = clock.instant(),
                        reasonCode = "ANNOUNCEMENT_DELETED",
                        beforeJson = announcementSnapshot(existing),
                    ),
                )
                idempotencyStore.complete(
                    actorUserId = command.actorUserId,
                    method = "DELETE",
                    pathTemplate = pathTemplate,
                    idempotencyKey = command.idempotencyKey,
                    status = 204,
                    body = Unit,
                )
            }
        }
    }

    private fun announcementSnapshot(announcement: Announcement): Map<String, Any?> =
        mapOf(
            "organizationId" to announcement.organizationId,
            "title" to announcement.title,
            "visibility" to announcement.visibility.name,
        )

    private fun requireAnnouncement(announcementId: Long): Announcement {
        return announcementRepository.findById(announcementId) ?: throw DomainException(
            errorCode = ErrorCode.VALIDATION_ERROR,
            status = 404,
            message = "announcement $announcementId not found",
        )
    }

    private fun paginate(
        announcements: List<Announcement>,
        cursor: String?,
        limit: Int,
    ): AnnouncementCursorPage {
        val safeLimit = limit.coerceIn(1, 100)
        val sorted = announcements.sortedWith(compareByDescending<Announcement> { it.updatedAt }.thenByDescending { it.id ?: -1L })
        val filtered =
            cursor?.toLongOrNull()?.let { cursorId ->
                sorted.dropWhile { (it.id ?: Long.MIN_VALUE) != cursorId }.drop(1)
            } ?: sorted
        val items = filtered.take(safeLimit)
        val nextCursor = items.takeIf { filtered.size > safeLimit }?.lastOrNull()?.id?.toString()
        return AnnouncementCursorPage(items = items, nextCursor = nextCursor)
    }

    private fun requestHash(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(input.toByteArray()).joinToString("") { "%02x".format(it) }
    }
}
