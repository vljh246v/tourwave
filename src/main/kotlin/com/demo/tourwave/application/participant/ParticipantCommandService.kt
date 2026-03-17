package com.demo.tourwave.application.participant

import com.demo.tourwave.application.booking.port.BookingRepository
import com.demo.tourwave.application.booking.port.OccurrenceRepository
import com.demo.tourwave.application.common.TimeWindowPolicyService
import com.demo.tourwave.application.common.port.AuditEventCommand
import com.demo.tourwave.application.common.port.AuditEventPort
import com.demo.tourwave.application.common.port.IdempotencyDecision
import com.demo.tourwave.application.common.port.IdempotencyStore
import com.demo.tourwave.application.participant.port.BookingParticipantRepository
import com.demo.tourwave.domain.common.DomainException
import com.demo.tourwave.domain.common.ErrorCode
import com.demo.tourwave.domain.booking.AttendanceStatus
import com.demo.tourwave.domain.participant.BookingParticipant
import java.security.MessageDigest
import java.time.Clock

class ParticipantCommandService(
    private val bookingRepository: BookingRepository,
    private val occurrenceRepository: OccurrenceRepository,
    private val bookingParticipantRepository: BookingParticipantRepository,
    private val participantInvitationLifecycleService: ParticipantInvitationLifecycleService,
    private val timeWindowPolicyService: TimeWindowPolicyService,
    private val idempotencyStore: IdempotencyStore,
    private val auditEventPort: AuditEventPort,
    private val clock: Clock
) {
    fun createInvitation(command: CreateParticipantInvitationCommand): CreateParticipantInvitationResult {
        val pathTemplate = "/bookings/{bookingId}/participants/invitations"
        val requestHash = hash("${command.bookingId}|${command.inviteeUserId}")

        return when (
            val decision = idempotencyStore.reserveOrReplay(
                actorUserId = command.actorUserId,
                method = "POST",
                pathTemplate = pathTemplate,
                idempotencyKey = command.idempotencyKey,
                requestHash = requestHash
            )
        ) {
            is IdempotencyDecision.Replay -> CreateParticipantInvitationResult(
                status = decision.status,
                invitation = decision.body as ParticipantInvitationCreated
            )

            IdempotencyDecision.Reserved -> {
                val booking = bookingRepository.findById(command.bookingId)
                    ?: throw DomainException(
                        errorCode = ErrorCode.VALIDATION_ERROR,
                        status = 422,
                        message = "Booking not found",
                        details = mapOf("bookingId" to command.bookingId)
                    )

                if (booking.leaderUserId != command.actorUserId) {
                    throw DomainException(
                        errorCode = ErrorCode.VALIDATION_ERROR,
                        status = 422,
                        message = "Only booking leader can invite participants",
                        details = mapOf("bookingId" to command.bookingId, "actorUserId" to command.actorUserId)
                    )
                }

                if (booking.status.isTerminal()) {
                    throw DomainException(
                        errorCode = ErrorCode.BOOKING_TERMINAL_STATE,
                        status = 409,
                        message = "Cannot invite participants for terminal booking",
                        details = mapOf("bookingId" to booking.id, "status" to booking.status)
                    )
                }

                val participants = participantInvitationLifecycleService.refreshBookingParticipants(command.bookingId)
                val occurrence = occurrenceRepository.getOrCreate(booking.occurrenceId)
                if (timeWindowPolicyService.isInvitationWindowClosed(occurrence, clock.instant())) {
                    throw DomainException(
                        errorCode = ErrorCode.INVALID_STATE_TRANSITION,
                        status = 409,
                        message = "Invitation window is closed",
                        details = mapOf("bookingId" to booking.id, "occurrenceId" to booking.occurrenceId)
                    )
                }
                if (participants.any { it.userId == command.inviteeUserId }) {
                    throw DomainException(
                        errorCode = ErrorCode.VALIDATION_ERROR,
                        status = 422,
                        message = "Participant already exists for this booking",
                        details = mapOf("bookingId" to command.bookingId, "userId" to command.inviteeUserId)
                    )
                }

                val activeParticipants = participants.count { it.isActive() }
                if (activeParticipants >= booking.partySize) {
                    throw DomainException(
                        errorCode = ErrorCode.VALIDATION_ERROR,
                        status = 422,
                        message = "Booking already has maximum participants for current party size",
                        details = mapOf(
                            "bookingId" to command.bookingId,
                            "partySize" to booking.partySize,
                            "activeParticipantCount" to activeParticipants
                        )
                    )
                }

                val created = bookingParticipantRepository.save(
                    BookingParticipant(
                        bookingId = command.bookingId,
                        userId = command.inviteeUserId,
                        status = com.demo.tourwave.domain.participant.BookingParticipantStatus.INVITED,
                        invitedAt = clock.instant(),
                        createdAt = clock.instant()
                    )
                )

                val response = ParticipantInvitationCreated(
                    id = requireNotNull(created.id),
                    bookingId = created.bookingId,
                    userId = created.userId,
                    status = created.status,
                    invitedAt = requireNotNull(created.invitedAt)
                )

                idempotencyStore.complete(
                    actorUserId = command.actorUserId,
                    method = "POST",
                    pathTemplate = pathTemplate,
                    idempotencyKey = command.idempotencyKey,
                    status = 201,
                    body = response
                )

                auditEventPort.append(
                    AuditEventCommand(
                        actor = "USER:${command.actorUserId}",
                        action = "PARTICIPANT_INVITED",
                        resourceType = "BOOKING_PARTICIPANT",
                        resourceId = response.id,
                        occurredAtUtc = clock.instant(),
                        requestId = command.requestId,
                        reasonCode = "PARTICIPANT_INVITATION_CREATED",
                        afterJson = participantSnapshot(created)
                    )
                )

                CreateParticipantInvitationResult(status = 201, invitation = response)
            }
        }
    }

    fun respondInvitation(command: RespondParticipantInvitationCommand): RespondParticipantInvitationResult {
        val pathTemplate = "/bookings/{bookingId}/participants/invitations/{participantId}/${command.responseType.name.lowercase()}"
        val requestHash = hash("${command.bookingId}|${command.participantId}|${command.responseType}")

        return when (
            val decision = idempotencyStore.reserveOrReplay(
                actorUserId = command.actorUserId,
                method = "POST",
                pathTemplate = pathTemplate,
                idempotencyKey = command.idempotencyKey,
                requestHash = requestHash
            )
        ) {
            is IdempotencyDecision.Replay -> RespondParticipantInvitationResult(
                status = decision.status,
                invitation = decision.body as ParticipantInvitationResponded
            )

            IdempotencyDecision.Reserved -> {
                val booking = bookingRepository.findById(command.bookingId)
                    ?: throw DomainException(
                        errorCode = ErrorCode.VALIDATION_ERROR,
                        status = 422,
                        message = "Booking not found",
                        details = mapOf("bookingId" to command.bookingId)
                    )

                if (booking.status.isTerminal()) {
                    throw DomainException(
                        errorCode = ErrorCode.BOOKING_TERMINAL_STATE,
                        status = 409,
                        message = "Cannot respond to invitation for terminal booking",
                        details = mapOf("bookingId" to booking.id, "status" to booking.status)
                    )
                }
                val refreshedParticipant = participantInvitationLifecycleService.refreshParticipant(
                    bookingId = command.bookingId,
                    participantId = command.participantId
                )
                val occurrence = occurrenceRepository.getOrCreate(booking.occurrenceId)
                if (timeWindowPolicyService.isInvitationWindowClosed(occurrence, clock.instant())) {
                    throw DomainException(
                        errorCode = ErrorCode.INVITATION_EXPIRED,
                        status = 409,
                        message = "Invitation response window is closed",
                        details = mapOf("participantId" to command.participantId, "bookingId" to booking.id)
                    )
                }

                val participant = bookingParticipantRepository.findById(command.participantId)
                    ?: throw DomainException(
                        errorCode = ErrorCode.VALIDATION_ERROR,
                        status = 422,
                        message = "Participant invitation not found",
                        details = mapOf("participantId" to command.participantId)
                    )
                val effectiveParticipant = refreshedParticipant ?: participant

                if (effectiveParticipant.bookingId != command.bookingId) {
                    throw DomainException(
                        errorCode = ErrorCode.BOOKING_SCOPE_MISMATCH,
                        status = 422,
                        message = "Participant does not belong to booking",
                        details = mapOf("bookingId" to command.bookingId, "participantId" to command.participantId)
                    )
                }

                if (effectiveParticipant.userId != command.actorUserId) {
                    throw DomainException(
                        errorCode = ErrorCode.VALIDATION_ERROR,
                        status = 422,
                        message = "Only invited participant can respond",
                        details = mapOf("participantId" to command.participantId, "actorUserId" to command.actorUserId)
                    )
                }

                if (effectiveParticipant.status == com.demo.tourwave.domain.participant.BookingParticipantStatus.EXPIRED) {
                    throw DomainException(
                        errorCode = ErrorCode.INVITATION_EXPIRED,
                        status = 409,
                        message = "Invitation is expired",
                        details = mapOf("participantId" to command.participantId)
                    )
                }

                if (effectiveParticipant.status != com.demo.tourwave.domain.participant.BookingParticipantStatus.INVITED) {
                    throw DomainException(
                        errorCode = ErrorCode.INVALID_STATE_TRANSITION,
                        status = 409,
                        message = "Invitation is not pending",
                        details = mapOf("participantId" to command.participantId, "status" to effectiveParticipant.status)
                    )
                }

                val updated = when (command.responseType) {
                    ParticipantInvitationResponseType.ACCEPT -> effectiveParticipant.accept(clock.instant())
                    ParticipantInvitationResponseType.DECLINE -> effectiveParticipant.decline(clock.instant())
                }
                val saved = bookingParticipantRepository.save(updated)

                val response = ParticipantInvitationResponded(
                    id = requireNotNull(saved.id),
                    bookingId = saved.bookingId,
                    userId = saved.userId,
                    status = saved.status,
                    respondedAt = requireNotNull(saved.respondedAt)
                )

                idempotencyStore.complete(
                    actorUserId = command.actorUserId,
                    method = "POST",
                    pathTemplate = pathTemplate,
                    idempotencyKey = command.idempotencyKey,
                    status = 200,
                    body = response
                )

                auditEventPort.append(
                    AuditEventCommand(
                        actor = "USER:${command.actorUserId}",
                        action = "PARTICIPANT_INVITATION_${command.responseType.name}",
                        resourceType = "BOOKING_PARTICIPANT",
                        resourceId = response.id,
                        occurredAtUtc = clock.instant(),
                        requestId = command.requestId,
                        reasonCode = "PARTICIPANT_INVITATION_${command.responseType.name}",
                        beforeJson = participantSnapshot(effectiveParticipant),
                        afterJson = participantSnapshot(saved)
                    )
                )

                RespondParticipantInvitationResult(status = 200, invitation = response)
            }
        }
    }

    fun recordAttendance(command: RecordParticipantAttendanceCommand): RecordParticipantAttendanceResult {
        val pathTemplate = "/bookings/{bookingId}/participants/{participantId}/attendance"
        val requestHash = hash("${command.bookingId}|${command.participantId}|${command.attendanceStatus}")

        return when (
            val decision = idempotencyStore.reserveOrReplay(
                actorUserId = command.actorUserId,
                method = "POST",
                pathTemplate = pathTemplate,
                idempotencyKey = command.idempotencyKey,
                requestHash = requestHash
            )
        ) {
            is IdempotencyDecision.Replay -> RecordParticipantAttendanceResult(
                status = decision.status,
                attendance = decision.body as ParticipantAttendanceRecorded
            )

            IdempotencyDecision.Reserved -> {
                bookingRepository.findById(command.bookingId)
                    ?: throw DomainException(
                        errorCode = ErrorCode.VALIDATION_ERROR,
                        status = 422,
                        message = "Booking not found",
                        details = mapOf("bookingId" to command.bookingId)
                    )

                val participant = bookingParticipantRepository.findById(command.participantId)
                    ?: throw DomainException(
                        errorCode = ErrorCode.VALIDATION_ERROR,
                        status = 422,
                        message = "Participant not found",
                        details = mapOf("participantId" to command.participantId)
                    )

                if (participant.bookingId != command.bookingId) {
                    throw DomainException(
                        errorCode = ErrorCode.BOOKING_SCOPE_MISMATCH,
                        status = 422,
                        message = "Participant does not belong to booking",
                        details = mapOf("bookingId" to command.bookingId, "participantId" to command.participantId)
                    )
                }

                if (command.attendanceStatus == AttendanceStatus.UNKNOWN) {
                    throw DomainException(
                        errorCode = ErrorCode.VALIDATION_ERROR,
                        status = 422,
                        message = "attendanceStatus must be explicit",
                        details = mapOf("attendanceStatus" to command.attendanceStatus)
                    )
                }

                val saved = bookingParticipantRepository.save(
                    participant.recordAttendance(command.attendanceStatus)
                )

                val response = ParticipantAttendanceRecorded(
                    id = requireNotNull(saved.id),
                    bookingId = saved.bookingId,
                    userId = saved.userId,
                    attendanceStatus = saved.attendanceStatus
                )

                idempotencyStore.complete(
                    actorUserId = command.actorUserId,
                    method = "POST",
                    pathTemplate = pathTemplate,
                    idempotencyKey = command.idempotencyKey,
                    status = 200,
                    body = response
                )

                auditEventPort.append(
                    AuditEventCommand(
                        actor = "USER:${command.actorUserId}",
                        action = "PARTICIPANT_ATTENDANCE_RECORDED",
                        resourceType = "BOOKING_PARTICIPANT",
                        resourceId = response.id,
                        occurredAtUtc = clock.instant(),
                        requestId = command.requestId,
                        reasonCode = "PARTICIPANT_ATTENDANCE_RECORDED",
                        beforeJson = participantSnapshot(participant),
                        afterJson = participantSnapshot(saved)
                    )
                )

                RecordParticipantAttendanceResult(status = 200, attendance = response)
            }
        }
    }

    private fun hash(raw: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun participantSnapshot(participant: BookingParticipant): Map<String, Any?> {
        return mapOf(
            "bookingId" to participant.bookingId,
            "userId" to participant.userId,
            "status" to participant.status.name,
            "attendanceStatus" to participant.attendanceStatus.name,
            "invitedAt" to participant.invitedAt?.toString(),
            "respondedAt" to participant.respondedAt?.toString()
        )
    }
}
