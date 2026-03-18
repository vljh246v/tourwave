package com.demo.tourwave.agent

import com.demo.tourwave.adapter.`in`.web.asset.AssetController
import com.demo.tourwave.adapter.`in`.web.booking.BookingRefundPreviewController
import com.demo.tourwave.adapter.`in`.web.booking.WaitlistOperatorController
import com.demo.tourwave.adapter.`in`.web.customer.CustomerController
import com.demo.tourwave.adapter.`in`.web.inquiry.InquiryQueryController
import com.demo.tourwave.adapter.`in`.web.instructor.InstructorProfileController
import com.demo.tourwave.adapter.`in`.web.instructor.InstructorRegistrationController
import com.demo.tourwave.adapter.`in`.web.organization.OrganizationOperatorController
import com.demo.tourwave.adapter.`in`.web.organization.OrganizationPublicController
import com.demo.tourwave.adapter.`in`.web.occurrence.OccurrenceOperatorController
import com.demo.tourwave.adapter.`in`.web.occurrence.OccurrencePublicController
import com.demo.tourwave.adapter.`in`.web.participant.ParticipantRosterController
import com.demo.tourwave.adapter.`in`.web.payment.PaymentOperatorController
import com.demo.tourwave.adapter.`in`.web.payment.PaymentWebhookController
import com.demo.tourwave.adapter.`in`.web.tour.TourOperatorController
import com.demo.tourwave.adapter.`in`.web.tour.TourPublicController
import org.junit.jupiter.api.Test
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DocumentationBaselineTest {
    @Test
    fun `api status matrix captures current runtime truth for drift-prone endpoints`() {
        val doc = readProjectFile("agent/13_api_status_matrix.md")

        assertContains(doc, "GET /bookings/{bookingId}/refund-preview")
        assertContains(doc, "POST /bookings/{bookingId}/waitlist/promote")
        assertContains(doc, "POST /bookings/{bookingId}/waitlist/skip")
        assertContains(doc, "GET /inquiries/{inquiryId}/messages")
        assertContains(doc, "GET /occurrences/{occurrenceId}/participants/roster/export")
        assertContains(doc, "현재 인증은 JWT access token 기반이고, local/test 런타임에서는 request header actor context fallback이 허용된다.")
        assertContains(doc, "POST /auth/signup")
        assertContains(doc, "GET /me")
        assertContains(doc, "POST /operator/organizations")
        assertContains(doc, "POST /organizations/{organizationId}/memberships/accept")
        assertContains(doc, "POST /instructor-registrations")
        assertContains(doc, "GET /me/instructor-profile?organizationId=...")
        assertContains(doc, "POST /organizations/{organizationId}/tours")
        assertContains(doc, "POST /tours/{tourId}/occurrences")
        assertContains(doc, "PATCH /occurrences/{occurrenceId}")
        assertContains(doc, "POST /occurrences/{occurrenceId}/reschedule")
        assertContains(doc, "GET /tours")
        assertContains(doc, "GET /tours/{tourId}")
        assertContains(doc, "GET /tours/{tourId}/content")
        assertContains(doc, "GET /tours/{tourId}/occurrences")
        assertContains(doc, "GET /occurrences/{occurrenceId}/availability")
        assertContains(doc, "GET /occurrences/{occurrenceId}/quote")
        assertContains(doc, "GET /search/occurrences")
        assertContains(doc, "POST /assets/uploads")
        assertContains(doc, "POST /assets/{assetId}/complete")
        assertContains(doc, "PUT /operator/organizations/{organizationId}/assets")
        assertContains(doc, "PUT /tours/{tourId}/assets")
        assertContains(doc, "GET /me/bookings")
        assertContains(doc, "GET /bookings/{bookingId}/calendar.ics")
        assertContains(doc, "GET /occurrences/{occurrenceId}/calendar.ics")
        assertContains(doc, "POST /tours/{tourId}/favorite")
        assertContains(doc, "DELETE /tours/{tourId}/favorite")
        assertContains(doc, "GET /me/favorites")
        assertContains(doc, "GET /me/notifications")
        assertContains(doc, "POST /me/notifications/{notificationId}/read")
        assertContains(doc, "POST /me/notifications/read-all")
        assertContains(doc, "POST /payments/webhooks/provider")
        assertContains(doc, "GET /operator/payments/refunds/ops")
        assertContains(doc, "POST /operator/payments/bookings/{bookingId}/refund-retry")
        assertContains(doc, "GET /operator/finance/reconciliation/daily")
        assertContains(doc, "POST /operator/finance/reconciliation/daily/{summaryDate}/refresh")
        assertContains(doc, "GET /operator/finance/reconciliation/daily/export")
        assertContains(doc, "GET /actuator/health")
        assertContains(doc, "GET /actuator/metrics/tourwave.job.execution")
        assertContains(doc, "/me`는 현재 사용자 profile과 organization memberships를 함께 반환한다.")
        assertContains(doc, "DocumentationBaselineTest")
    }

    @Test
    fun `spec and catalog docs encode sprint 7 governance rules`() {
        val specIndex = readProjectFile("agent/09_spec_index.md")
        val apiCatalog = readProjectFile("agent/03_api_catalog.md")
        val traceability = readProjectFile("agent/14_test_traceability_matrix.md")
        val gapArchive = readProjectFile("docs/openapi-gap-report.md")
        val runbook = readProjectFile("docs/launch-readiness-checklist.md")

        assertContains(specIndex, "16_product_delivery_roadmap.md")
        assertContains(specIndex, "launch-readiness-checklist.md")
        assertContains(specIndex, "13_api_status_matrix.md")
        assertContains(specIndex, "실무 순서:")
        assertContains(apiCatalog, "Target Product Auth: Bearer JWT")
        assertContains(apiCatalog, "Current Runtime Auth: Bearer JWT, with request header actor context fallback only in local/test flows")
        assertContains(apiCatalog, "POST /operator/organizations")
        assertContains(apiCatalog, "POST /organizations/{orgId}/memberships/accept")
        assertContains(apiCatalog, "GET /me/instructor-profile?organizationId={orgId}")
        assertContains(apiCatalog, "POST /organizations/{orgId}/tours")
        assertContains(apiCatalog, "POST /tours/{tourId}/occurrences")
        assertContains(apiCatalog, "GET /tours")
        assertContains(apiCatalog, "GET /occurrences/{occurrenceId}/quote")
        assertContains(apiCatalog, "POST /assets/uploads")
        assertContains(apiCatalog, "GET /me/bookings")
        assertContains(apiCatalog, "GET /me/favorites")
        assertContains(apiCatalog, "GET /me/notifications")
        assertContains(apiCatalog, "POST /payments/webhooks/provider")
        assertContains(apiCatalog, "GET /operator/payments/refunds/ops")
        assertContains(apiCatalog, "GET /operator/finance/reconciliation/daily")
        assertContains(apiCatalog, "Current runtime note:")
        assertContains(apiCatalog, "1차 확인: controller + integration test")
        assertContains(traceability, "DocumentationBaselineTest")
        assertContains(traceability, "OrganizationControllerIntegrationTest")
        assertContains(traceability, "InstructorAndTourControllerIntegrationTest")
        assertContains(traceability, "OccurrenceCatalogControllerIntegrationTest")
        assertContains(traceability, "CustomerControllerIntegrationTest")
        assertContains(traceability, "PaymentControllerIntegrationTest")
        assertContains(traceability, "OperationalActuatorIntegrationTest")
        assertContains(traceability, "OpenApiContractVerificationTest")
        assertContains(traceability, "RealMysqlContainerSmokeTest")
        assertContains(gapArchive, "Historical Archive")
        assertContains(gapArchive, "현재 truth 확인 순서:")
        assertContains(runbook, "Launch Readiness Checklist")
        assertContains(runbook, "worker distributed lock")
    }

    @Test
    fun `drift-prone controller mappings stay aligned with current truth docs`() {
        assertGetMapping(
            controller = BookingRefundPreviewController::class.java,
            methodName = "getRefundPreview",
            expectedPath = "/bookings/{bookingId}/refund-preview"
        )
        assertPostMapping(
            controller = WaitlistOperatorController::class.java,
            methodName = "promote",
            expectedPath = "/bookings/{bookingId}/waitlist/promote"
        )
        assertPostMapping(
            controller = WaitlistOperatorController::class.java,
            methodName = "skip",
            expectedPath = "/bookings/{bookingId}/waitlist/skip"
        )
        assertGetMapping(
            controller = InquiryQueryController::class.java,
            methodName = "listInquiryMessages",
            expectedPath = "/inquiries/{inquiryId}/messages"
        )
        assertGetMapping(
            controller = ParticipantRosterController::class.java,
            methodName = "exportRoster",
            expectedPath = "/occurrences/{occurrenceId}/participants/roster/export"
        )
        assertPostMapping(
            controller = OrganizationOperatorController::class.java,
            methodName = "createOrganization",
            expectedPath = "/operator/organizations"
        )
        assertGetMapping(
            controller = OrganizationPublicController::class.java,
            methodName = "getPublicOrganization",
            expectedPath = "/organizations/{organizationId}"
        )
        assertPostMapping(
            controller = OrganizationPublicController::class.java,
            methodName = "acceptInvitation",
            expectedPath = "/organizations/{organizationId}/memberships/accept"
        )
        assertPostMapping(
            controller = InstructorRegistrationController::class.java,
            methodName = "apply",
            expectedPath = "/instructor-registrations"
        )
        assertGetMapping(
            controller = InstructorRegistrationController::class.java,
            methodName = "listByOrganization",
            expectedPath = "/organizations/{organizationId}/instructor-registrations"
        )
        assertPostMapping(
            controller = InstructorRegistrationController::class.java,
            methodName = "approve",
            expectedPath = "/instructor-registrations/{registrationId}/approve"
        )
        assertGetMapping(
            controller = InstructorProfileController::class.java,
            methodName = "getMyProfile",
            expectedPath = "/me/instructor-profile"
        )
        assertPatchMapping(
            controller = InstructorProfileController::class.java,
            methodName = "updateMyProfile",
            expectedPath = "/me/instructor-profile"
        )
        assertGetMapping(
            controller = InstructorProfileController::class.java,
            methodName = "getPublicProfile",
            expectedPath = "/instructors/{instructorProfileId}"
        )
        assertPostMapping(
            controller = TourOperatorController::class.java,
            methodName = "createTour",
            expectedPath = "/organizations/{organizationId}/tours"
        )
        assertPostMapping(
            controller = OccurrenceOperatorController::class.java,
            methodName = "createOccurrence",
            expectedPath = "/tours/{tourId}/occurrences"
        )
        assertPatchMapping(
            controller = OccurrenceOperatorController::class.java,
            methodName = "updateOccurrence",
            expectedPath = "/occurrences/{occurrenceId}"
        )
        assertPostMapping(
            controller = OccurrenceOperatorController::class.java,
            methodName = "rescheduleOccurrence",
            expectedPath = "/occurrences/{occurrenceId}/reschedule"
        )
        assertGetMapping(
            controller = TourPublicController::class.java,
            methodName = "listTours",
            expectedPath = "/tours"
        )
        assertGetMapping(
            controller = TourPublicController::class.java,
            methodName = "getTour",
            expectedPath = "/tours/{tourId}"
        )
        assertGetMapping(
            controller = TourPublicController::class.java,
            methodName = "listOccurrences",
            expectedPath = "/tours/{tourId}/occurrences"
        )
        assertPutMapping(
            controller = TourOperatorController::class.java,
            methodName = "updateTourContent",
            expectedPath = "/tours/{tourId}/content"
        )
        assertGetMapping(
            controller = TourPublicController::class.java,
            methodName = "getPublicContent",
            expectedPath = "/tours/{tourId}/content"
        )
        assertGetMapping(
            controller = OccurrencePublicController::class.java,
            methodName = "getAvailability",
            expectedPath = "/occurrences/{occurrenceId}/availability"
        )
        assertGetMapping(
            controller = OccurrencePublicController::class.java,
            methodName = "getQuote",
            expectedPath = "/occurrences/{occurrenceId}/quote"
        )
        assertGetMapping(
            controller = OccurrencePublicController::class.java,
            methodName = "search",
            expectedPath = "/search/occurrences"
        )
        assertPostMapping(
            controller = AssetController::class.java,
            methodName = "issueUpload",
            expectedPath = "/assets/uploads"
        )
        assertPostMapping(
            controller = AssetController::class.java,
            methodName = "completeUpload",
            expectedPath = "/assets/{assetId}/complete"
        )
        assertPutMapping(
            controller = AssetController::class.java,
            methodName = "attachOrganizationAssets",
            expectedPath = "/operator/organizations/{organizationId}/assets"
        )
        assertPutMapping(
            controller = AssetController::class.java,
            methodName = "attachTourAssets",
            expectedPath = "/tours/{tourId}/assets"
        )
        assertGetMapping(
            controller = CustomerController::class.java,
            methodName = "listMyBookings",
            expectedPath = "/me/bookings"
        )
        assertGetMapping(
            controller = CustomerController::class.java,
            methodName = "bookingCalendar",
            expectedPath = "/bookings/{bookingId}/calendar.ics"
        )
        assertGetMapping(
            controller = CustomerController::class.java,
            methodName = "occurrenceCalendar",
            expectedPath = "/occurrences/{occurrenceId}/calendar.ics"
        )
        assertPostMapping(
            controller = CustomerController::class.java,
            methodName = "favoriteTour",
            expectedPath = "/tours/{tourId}/favorite"
        )
        assertDeleteMapping(
            controller = CustomerController::class.java,
            methodName = "unfavoriteTour",
            expectedPath = "/tours/{tourId}/favorite"
        )
        assertGetMapping(
            controller = CustomerController::class.java,
            methodName = "listFavorites",
            expectedPath = "/me/favorites"
        )
        assertGetMapping(
            controller = CustomerController::class.java,
            methodName = "listNotifications",
            expectedPath = "/me/notifications"
        )
        assertPostMapping(
            controller = CustomerController::class.java,
            methodName = "markNotificationRead",
            expectedPath = "/me/notifications/{notificationId}/read"
        )
        assertPostMapping(
            controller = CustomerController::class.java,
            methodName = "markAllNotificationsRead",
            expectedPath = "/me/notifications/read-all"
        )
        assertPostMapping(
            controller = PaymentWebhookController::class.java,
            methodName = "receiveProviderWebhook",
            expectedPath = "/payments/webhooks/provider"
        )
        assertGetMapping(
            controller = PaymentOperatorController::class.java,
            methodName = "listRefundOpsQueue",
            expectedPath = "/operator/payments/refunds/ops"
        )
        assertPostMapping(
            controller = PaymentOperatorController::class.java,
            methodName = "retryBookingRefund",
            expectedPath = "/operator/payments/bookings/{bookingId}/refund-retry"
        )
        assertGetMapping(
            controller = PaymentOperatorController::class.java,
            methodName = "listDailySummaries",
            expectedPath = "/operator/finance/reconciliation/daily"
        )
        assertPostMapping(
            controller = PaymentOperatorController::class.java,
            methodName = "refreshDailySummary",
            expectedPath = "/operator/finance/reconciliation/daily/{summaryDate}/refresh"
        )
        assertGetMapping(
            controller = PaymentOperatorController::class.java,
            methodName = "exportDailySummariesCsv",
            expectedPath = "/operator/finance/reconciliation/daily/export"
        )
    }

    private fun assertGetMapping(controller: Class<*>, methodName: String, expectedPath: String) {
        val annotation = controller.declaredMethods.single { it.name == methodName }.getAnnotation(GetMapping::class.java)
        requireNotNull(annotation) { "Expected @GetMapping on ${controller.simpleName}.$methodName" }
        assertEquals(expectedPath, annotation.paths().singlePath())
    }

    private fun assertPostMapping(controller: Class<*>, methodName: String, expectedPath: String) {
        val annotation = controller.declaredMethods.single { it.name == methodName }.getAnnotation(PostMapping::class.java)
        requireNotNull(annotation) { "Expected @PostMapping on ${controller.simpleName}.$methodName" }
        assertEquals(expectedPath, annotation.paths().singlePath())
    }

    private fun assertPatchMapping(controller: Class<*>, methodName: String, expectedPath: String) {
        val annotation = controller.declaredMethods.single { it.name == methodName }.getAnnotation(PatchMapping::class.java)
        requireNotNull(annotation) { "Expected @PatchMapping on ${controller.simpleName}.$methodName" }
        assertEquals(expectedPath, annotation.paths().singlePath())
    }

    private fun assertPutMapping(controller: Class<*>, methodName: String, expectedPath: String) {
        val annotation = controller.declaredMethods.single { it.name == methodName }.getAnnotation(PutMapping::class.java)
        requireNotNull(annotation) { "Expected @PutMapping on ${controller.simpleName}.$methodName" }
        assertEquals(expectedPath, annotation.paths().singlePath())
    }

    private fun assertDeleteMapping(controller: Class<*>, methodName: String, expectedPath: String) {
        val annotation = controller.declaredMethods.single { it.name == methodName }.getAnnotation(DeleteMapping::class.java)
        requireNotNull(annotation) { "Expected @DeleteMapping on ${controller.simpleName}.$methodName" }
        val paths = if (annotation.path.isNotEmpty()) annotation.path else annotation.value
        assertEquals(expectedPath, paths.singlePath())
    }

    private fun readProjectFile(relativePath: String): String = Files.readString(Path.of(relativePath))

    private fun assertContains(content: String, expectedSnippet: String) {
        assertTrue(
            content.contains(expectedSnippet),
            "Expected snippet not found: $expectedSnippet"
        )
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
