package com.demo.tourwave.agent

import com.demo.tourwave.adapter.`in`.web.booking.BookingRefundPreviewController
import com.demo.tourwave.adapter.`in`.web.booking.WaitlistOperatorController
import com.demo.tourwave.adapter.`in`.web.inquiry.InquiryQueryController
import com.demo.tourwave.adapter.`in`.web.organization.OrganizationOperatorController
import com.demo.tourwave.adapter.`in`.web.organization.OrganizationPublicController
import com.demo.tourwave.adapter.`in`.web.participant.ParticipantRosterController
import org.junit.jupiter.api.Test
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
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
        assertContains(doc, "/me`는 현재 사용자 profile과 organization memberships를 함께 반환한다.")
        assertContains(doc, "DocumentationBaselineTest")
    }

    @Test
    fun `spec and catalog docs encode sprint 7 governance rules`() {
        val specIndex = readProjectFile("agent/09_spec_index.md")
        val apiCatalog = readProjectFile("agent/03_api_catalog.md")
        val traceability = readProjectFile("agent/14_test_traceability_matrix.md")
        val gapArchive = readProjectFile("docs/openapi-gap-report.md")

        assertContains(specIndex, "16_product_delivery_roadmap.md")
        assertContains(specIndex, "13_api_status_matrix.md")
        assertContains(specIndex, "실무 순서:")
        assertContains(apiCatalog, "Target Product Auth: Bearer JWT")
        assertContains(apiCatalog, "Current Runtime Auth: Bearer JWT, with request header actor context fallback only in local/test flows")
        assertContains(apiCatalog, "POST /operator/organizations")
        assertContains(apiCatalog, "POST /organizations/{orgId}/memberships/accept")
        assertContains(apiCatalog, "1차 확인: controller + integration test")
        assertContains(traceability, "DocumentationBaselineTest")
        assertContains(traceability, "OrganizationControllerIntegrationTest")
        assertContains(gapArchive, "Historical Archive")
        assertContains(gapArchive, "현재 truth 확인 순서:")
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

    private fun readProjectFile(relativePath: String): String = Files.readString(Path.of(relativePath))

    private fun assertContains(content: String, expectedSnippet: String) {
        assertTrue(
            content.contains(expectedSnippet),
            "Expected snippet not found: $expectedSnippet"
        )
    }

    private fun GetMapping.paths(): Array<String> = if (path.isNotEmpty()) path else value

    private fun PostMapping.paths(): Array<String> = if (path.isNotEmpty()) path else value

    private fun Array<String>.singlePath(): String {
        assertEquals(1, size, "Expected a single mapping path")
        return single()
    }
}
