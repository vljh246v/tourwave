package com.demo.tourwave.application.inquiry

import com.demo.tourwave.application.booking.port.BookingRepository
import com.demo.tourwave.domain.common.DomainException
import com.demo.tourwave.domain.common.ErrorCode
import com.demo.tourwave.domain.inquiry.Inquiry

enum class InquiryAccessType {
    BOOKING_LEADER,
    ORG_OPERATOR
}

data class InquiryActorContext(
    val actorUserId: Long,
    val actorOrgRole: String? = null,
    val actorOrgId: Long? = null,
    val requestId: String? = null
)

class InquiryAccessPolicy(
    private val bookingRepository: BookingRepository
) {
    fun authorize(inquiry: Inquiry, actor: InquiryActorContext): InquiryAccessType {
        val booking = bookingRepository.findById(inquiry.bookingId)
            ?: throw DomainException(
                errorCode = ErrorCode.BOOKING_SCOPE_MISMATCH,
                status = 422,
                message = "inquiry booking scope is invalid",
                details = mapOf("inquiryId" to inquiry.id, "bookingId" to inquiry.bookingId)
            )

        if (booking.organizationId != inquiry.organizationId || booking.occurrenceId != inquiry.occurrenceId) {
            throw DomainException(
                errorCode = ErrorCode.BOOKING_SCOPE_MISMATCH,
                status = 422,
                message = "inquiry booking scope is invalid",
                details = mapOf(
                    "inquiryId" to inquiry.id,
                    "bookingId" to inquiry.bookingId,
                    "bookingOccurrenceId" to booking.occurrenceId,
                    "inquiryOccurrenceId" to inquiry.occurrenceId,
                    "bookingOrganizationId" to booking.organizationId,
                    "inquiryOrganizationId" to inquiry.organizationId
                )
            )
        }

        if (booking.leaderUserId == actor.actorUserId) {
            return InquiryAccessType.BOOKING_LEADER
        }

        val normalizedRole = actor.actorOrgRole?.uppercase()
        if (normalizedRole == "ORG_ADMIN" || normalizedRole == "ORG_OWNER") {
            val actorOrgId = actor.actorOrgId
                ?: throw DomainException(
                    errorCode = ErrorCode.REQUIRED_FIELD_MISSING,
                    status = 422,
                    message = "X-Actor-Org-Id is required for org operator access",
                    details = mapOf("field" to "X-Actor-Org-Id")
                )

            if (actorOrgId != inquiry.organizationId) {
                throw DomainException(
                    errorCode = ErrorCode.BOOKING_SCOPE_MISMATCH,
                    status = 422,
                    message = "operator organization does not match inquiry scope",
                    details = mapOf(
                        "inquiryId" to inquiry.id,
                        "inquiryOrganizationId" to inquiry.organizationId,
                        "actorOrganizationId" to actorOrgId
                    )
                )
            }

            return InquiryAccessType.ORG_OPERATOR
        }

        throw DomainException(
            errorCode = ErrorCode.VALIDATION_ERROR,
            status = 403,
            message = "Forbidden inquiry access",
            details = mapOf("inquiryId" to inquiry.id, "actorUserId" to actor.actorUserId)
        )
    }
}

