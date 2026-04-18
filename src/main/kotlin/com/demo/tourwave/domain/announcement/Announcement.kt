package com.demo.tourwave.domain.announcement

import java.time.Instant

enum class AnnouncementVisibility {
    DRAFT,
    PUBLIC,
    INTERNAL,
}

data class Announcement(
    val id: Long? = null,
    val organizationId: Long,
    val title: String,
    val body: String,
    val visibility: AnnouncementVisibility = AnnouncementVisibility.DRAFT,
    val publishStartsAtUtc: Instant? = null,
    val publishEndsAtUtc: Instant? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    init {
        require(title.isNotBlank()) { "title must not be blank" }
        require(title.length <= 200) { "title length must be <= 200" }
        require(body.isNotBlank()) { "body must not be blank" }
        require(body.length <= 5000) { "body length must be <= 5000" }
        if (publishStartsAtUtc != null && publishEndsAtUtc != null) {
            require(!publishEndsAtUtc.isBefore(publishStartsAtUtc)) { "publish window is invalid" }
        }
    }

    fun update(
        title: String = this.title,
        body: String = this.body,
        visibility: AnnouncementVisibility = this.visibility,
        publishStartsAtUtc: Instant? = this.publishStartsAtUtc,
        publishEndsAtUtc: Instant? = this.publishEndsAtUtc,
        now: Instant,
    ): Announcement {
        return copy(
            title = title,
            body = body,
            visibility = visibility,
            publishStartsAtUtc = publishStartsAtUtc,
            publishEndsAtUtc = publishEndsAtUtc,
            updatedAt = now,
        )
    }

    fun isVisibleToPublic(now: Instant): Boolean {
        if (visibility != AnnouncementVisibility.PUBLIC) {
            return false
        }
        if (publishStartsAtUtc != null && now.isBefore(publishStartsAtUtc)) {
            return false
        }
        if (publishEndsAtUtc != null && !now.isBefore(publishEndsAtUtc)) {
            return false
        }
        return true
    }

    companion object {
        fun create(
            organizationId: Long,
            title: String,
            body: String,
            visibility: AnnouncementVisibility,
            publishStartsAtUtc: Instant?,
            publishEndsAtUtc: Instant?,
            now: Instant,
        ): Announcement {
            return Announcement(
                organizationId = organizationId,
                title = title,
                body = body,
                visibility = visibility,
                publishStartsAtUtc = publishStartsAtUtc,
                publishEndsAtUtc = publishEndsAtUtc,
                createdAt = now,
                updatedAt = now,
            )
        }
    }
}
