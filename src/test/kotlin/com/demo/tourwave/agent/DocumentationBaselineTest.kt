package com.demo.tourwave.agent

import com.demo.tourwave.adapter.`in`.web.announcement.AnnouncementController
import com.demo.tourwave.adapter.`in`.web.asset.AssetController
import com.demo.tourwave.adapter.`in`.web.booking.BookingRefundPreviewController
import com.demo.tourwave.adapter.`in`.web.booking.WaitlistOperatorController
import com.demo.tourwave.adapter.`in`.web.customer.CustomerController
import com.demo.tourwave.adapter.`in`.web.inquiry.InquiryQueryController
import com.demo.tourwave.adapter.`in`.web.instructor.InstructorProfileController
import com.demo.tourwave.adapter.`in`.web.instructor.InstructorRegistrationController
import com.demo.tourwave.adapter.`in`.web.occurrence.OccurrenceOperatorController
import com.demo.tourwave.adapter.`in`.web.occurrence.OccurrencePublicController
import com.demo.tourwave.adapter.`in`.web.operations.OperatorRemediationQueueController
import com.demo.tourwave.adapter.`in`.web.organization.OrganizationOperatorController
import com.demo.tourwave.adapter.`in`.web.organization.OrganizationPublicController
import com.demo.tourwave.adapter.`in`.web.participant.ParticipantRosterController
import com.demo.tourwave.adapter.`in`.web.payment.PaymentOperatorController
import com.demo.tourwave.adapter.`in`.web.payment.PaymentWebhookController
import com.demo.tourwave.adapter.`in`.web.reporting.OrganizationReportController
import com.demo.tourwave.adapter.`in`.web.review.ReviewController
import com.demo.tourwave.adapter.`in`.web.tour.TourOperatorController
import com.demo.tourwave.adapter.`in`.web.tour.TourPublicController
import org.junit.jupiter.api.Test
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import kotlin.test.assertEquals

class DocumentationBaselineTest {
    @Test
    fun `drift-prone controller mappings stay aligned with current truth docs`() {
        assertGetMapping(BookingRefundPreviewController::class.java, "getRefundPreview", "/bookings/{bookingId}/refund-preview")
        assertPostMapping(WaitlistOperatorController::class.java, "promote", "/bookings/{bookingId}/waitlist/promote")
        assertPostMapping(WaitlistOperatorController::class.java, "skip", "/bookings/{bookingId}/waitlist/skip")
        assertGetMapping(InquiryQueryController::class.java, "listInquiryMessages", "/inquiries/{inquiryId}/messages")
        assertGetMapping(ParticipantRosterController::class.java, "exportRoster", "/occurrences/{occurrenceId}/participants/roster/export")
        assertPostMapping(OrganizationOperatorController::class.java, "createOrganization", "/operator/organizations")
        assertGetMapping(AnnouncementController::class.java, "listPublicAnnouncements", "/public/announcements")
        assertPostMapping(AnnouncementController::class.java, "createAnnouncement", "/organizations/{organizationId}/announcements")
        assertPatchMapping(AnnouncementController::class.java, "updateAnnouncement", "/announcements/{announcementId}")
        assertDeleteMapping(AnnouncementController::class.java, "deleteAnnouncement", "/announcements/{announcementId}")
        assertGetMapping(OrganizationReportController::class.java, "getBookingReport", "/organizations/{organizationId}/reports/bookings")
        assertGetMapping(
            OrganizationReportController::class.java,
            "getOccurrenceOpsReport",
            "/organizations/{organizationId}/reports/occurrences",
        )
        assertGetMapping(OperatorRemediationQueueController::class.java, "listQueue", "/operator/operations/remediation-queue")
        assertPostMapping(
            OperatorRemediationQueueController::class.java,
            "remediate",
            "/operator/operations/remediation-queue/{sourceType}/{sourceKey}",
        )
        assertGetMapping(ReviewController::class.java, "getTourSummary", "/tours/{tourId}/reviews/summary")
        assertGetMapping(ReviewController::class.java, "getInstructorSummary", "/instructors/{instructorProfileId}/reviews/summary")
        assertGetMapping(ReviewController::class.java, "getPublicOrganizationSummary", "/organizations/{organizationId}/reviews/summary")
        assertGetMapping(
            ReviewController::class.java,
            "getOperatorOrganizationSummary",
            "/operator/organizations/{organizationId}/reviews/summary",
        )
        assertGetMapping(OrganizationPublicController::class.java, "getPublicOrganization", "/organizations/{organizationId}")
        assertPostMapping(
            OrganizationPublicController::class.java,
            "acceptInvitation",
            "/organizations/{organizationId}/memberships/accept",
        )
        assertPostMapping(InstructorRegistrationController::class.java, "apply", "/instructor-registrations")
        assertGetMapping(
            InstructorRegistrationController::class.java,
            "listByOrganization",
            "/organizations/{organizationId}/instructor-registrations",
        )
        assertPostMapping(InstructorRegistrationController::class.java, "approve", "/instructor-registrations/{registrationId}/approve")
        assertGetMapping(InstructorProfileController::class.java, "getMyProfile", "/me/instructor-profile")
        assertPatchMapping(InstructorProfileController::class.java, "updateMyProfile", "/me/instructor-profile")
        assertGetMapping(InstructorProfileController::class.java, "getPublicProfile", "/instructors/{instructorProfileId}")
        assertPostMapping(TourOperatorController::class.java, "createTour", "/organizations/{organizationId}/tours")
        assertPostMapping(OccurrenceOperatorController::class.java, "createOccurrence", "/tours/{tourId}/occurrences")
        assertPatchMapping(OccurrenceOperatorController::class.java, "updateOccurrence", "/occurrences/{occurrenceId}")
        assertPostMapping(OccurrenceOperatorController::class.java, "rescheduleOccurrence", "/occurrences/{occurrenceId}/reschedule")
        assertGetMapping(TourPublicController::class.java, "listTours", "/tours")
        assertGetMapping(TourPublicController::class.java, "getTour", "/tours/{tourId}")
        assertGetMapping(TourPublicController::class.java, "listOccurrences", "/tours/{tourId}/occurrences")
        assertPutMapping(TourOperatorController::class.java, "updateTourContent", "/tours/{tourId}/content")
        assertGetMapping(TourPublicController::class.java, "getPublicContent", "/tours/{tourId}/content")
        assertGetMapping(OccurrencePublicController::class.java, "getAvailability", "/occurrences/{occurrenceId}/availability")
        assertGetMapping(OccurrencePublicController::class.java, "getQuote", "/occurrences/{occurrenceId}/quote")
        assertGetMapping(OccurrencePublicController::class.java, "search", "/search/occurrences")
        assertPostMapping(AssetController::class.java, "issueUpload", "/assets/uploads")
        assertPostMapping(AssetController::class.java, "completeUpload", "/assets/{assetId}/complete")
        assertPutMapping(AssetController::class.java, "attachOrganizationAssets", "/operator/organizations/{organizationId}/assets")
        assertPutMapping(AssetController::class.java, "attachTourAssets", "/tours/{tourId}/assets")
        assertGetMapping(CustomerController::class.java, "listMyBookings", "/me/bookings")
        assertGetMapping(CustomerController::class.java, "bookingCalendar", "/bookings/{bookingId}/calendar.ics")
        assertGetMapping(CustomerController::class.java, "occurrenceCalendar", "/occurrences/{occurrenceId}/calendar.ics")
        assertPostMapping(CustomerController::class.java, "favoriteTour", "/tours/{tourId}/favorite")
        assertDeleteMapping(CustomerController::class.java, "unfavoriteTour", "/tours/{tourId}/favorite")
        assertGetMapping(CustomerController::class.java, "listFavorites", "/me/favorites")
        assertGetMapping(CustomerController::class.java, "listNotifications", "/me/notifications")
        assertPostMapping(CustomerController::class.java, "markNotificationRead", "/me/notifications/{notificationId}/read")
        assertPostMapping(CustomerController::class.java, "markAllNotificationsRead", "/me/notifications/read-all")
        assertPostMapping(PaymentWebhookController::class.java, "receiveProviderWebhook", "/payments/webhooks/provider")
        assertGetMapping(PaymentOperatorController::class.java, "listRefundOpsQueue", "/operator/payments/refunds/ops")
        assertPostMapping(
            PaymentOperatorController::class.java,
            "retryBookingRefund",
            "/operator/payments/bookings/{bookingId}/refund-retry",
        )
        assertGetMapping(PaymentOperatorController::class.java, "listDailySummaries", "/operator/finance/reconciliation/daily")
        assertPostMapping(
            PaymentOperatorController::class.java,
            "refreshDailySummary",
            "/operator/finance/reconciliation/daily/{summaryDate}/refresh",
        )
        assertGetMapping(PaymentOperatorController::class.java, "exportDailySummariesCsv", "/operator/finance/reconciliation/daily/export")
    }

    private fun assertGetMapping(
        controller: Class<*>,
        methodName: String,
        expectedPath: String,
    ) {
        val annotation = controller.declaredMethods.single { it.name == methodName }.getAnnotation(GetMapping::class.java)
        requireNotNull(annotation) { "Expected @GetMapping on ${controller.simpleName}.$methodName" }
        assertEquals(expectedPath, annotation.paths().singlePath())
    }

    private fun assertPostMapping(
        controller: Class<*>,
        methodName: String,
        expectedPath: String,
    ) {
        val annotation = controller.declaredMethods.single { it.name == methodName }.getAnnotation(PostMapping::class.java)
        requireNotNull(annotation) { "Expected @PostMapping on ${controller.simpleName}.$methodName" }
        assertEquals(expectedPath, annotation.paths().singlePath())
    }

    private fun assertPatchMapping(
        controller: Class<*>,
        methodName: String,
        expectedPath: String,
    ) {
        val annotation = controller.declaredMethods.single { it.name == methodName }.getAnnotation(PatchMapping::class.java)
        requireNotNull(annotation) { "Expected @PatchMapping on ${controller.simpleName}.$methodName" }
        assertEquals(expectedPath, annotation.paths().singlePath())
    }

    private fun assertPutMapping(
        controller: Class<*>,
        methodName: String,
        expectedPath: String,
    ) {
        val annotation = controller.declaredMethods.single { it.name == methodName }.getAnnotation(PutMapping::class.java)
        requireNotNull(annotation) { "Expected @PutMapping on ${controller.simpleName}.$methodName" }
        assertEquals(expectedPath, annotation.paths().singlePath())
    }

    private fun assertDeleteMapping(
        controller: Class<*>,
        methodName: String,
        expectedPath: String,
    ) {
        val annotation = controller.declaredMethods.single { it.name == methodName }.getAnnotation(DeleteMapping::class.java)
        requireNotNull(annotation) { "Expected @DeleteMapping on ${controller.simpleName}.$methodName" }
        val paths = if (annotation.path.isNotEmpty()) annotation.path else annotation.value
        assertEquals(expectedPath, paths.singlePath())
    }

    private fun GetMapping.paths(): Array<String> = if (path.isNotEmpty()) path else value

    private fun PostMapping.paths(): Array<String> = if (path.isNotEmpty()) path else value

    private fun PatchMapping.paths(): Array<String> = if (path.isNotEmpty()) path else value

    private fun PutMapping.paths(): Array<String> = if (path.isNotEmpty()) path else value

    private fun Array<String>.singlePath(): String {
        assertEquals(1, size, "Expected a single mapping path")
        return single()
    }
}
