package com.demo.tourwave.bootstrap

import com.demo.tourwave.application.booking.port.PaymentRecordRepository
import com.demo.tourwave.application.common.JobExecutionMonitor
import com.demo.tourwave.application.common.ScheduledJobCoordinator
import com.demo.tourwave.application.common.port.AuditEventPort
import com.demo.tourwave.application.common.port.IdempotencyStore
import com.demo.tourwave.application.common.port.WorkerJobLockRepository
import com.demo.tourwave.application.customer.NotificationDeliveryService
import com.demo.tourwave.application.customer.port.NotificationDeliveryRepository
import com.demo.tourwave.application.operations.OperatorRemediationQueueService
import com.demo.tourwave.application.operations.port.OperatorFailureRecordRepository
import com.demo.tourwave.application.payment.PaymentWebhookService
import com.demo.tourwave.application.payment.RefundOperationsService
import com.demo.tourwave.application.payment.port.PaymentProviderEventRepository
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.net.InetAddress
import java.time.Clock
import java.time.Duration
import java.util.UUID

@Configuration
class OperationsConfig {
    @Bean
    fun jobExecutionMonitor(): JobExecutionMonitor = JobExecutionMonitor()

    @Bean
    fun scheduledJobCoordinator(
        workerJobLockRepository: WorkerJobLockRepository,
        jobExecutionMonitor: JobExecutionMonitor,
        meterRegistry: MeterRegistry,
        clock: Clock,
        @Value("\${tourwave.jobs.lock.owner-id:}")
        configuredOwnerId: String,
        @Value("\${tourwave.jobs.lock.lease-seconds:120}")
        leaseSeconds: Long,
    ): ScheduledJobCoordinator {
        return ScheduledJobCoordinator(
            workerJobLockRepository = workerJobLockRepository,
            jobExecutionMonitor = jobExecutionMonitor,
            meterRegistry = meterRegistry,
            clock = clock,
            ownerId = resolveOwnerId(configuredOwnerId),
            leaseDuration = Duration.ofSeconds(leaseSeconds),
        )
    }

    @Bean
    fun operatorRemediationQueueService(
        paymentRecordRepository: PaymentRecordRepository,
        refundOperationsService: RefundOperationsService,
        notificationDeliveryRepository: NotificationDeliveryRepository,
        notificationDeliveryService: NotificationDeliveryService,
        paymentProviderEventRepository: PaymentProviderEventRepository,
        paymentWebhookService: PaymentWebhookService,
        operatorFailureRecordRepository: OperatorFailureRecordRepository,
        auditEventPort: AuditEventPort,
        idempotencyStore: IdempotencyStore,
        clock: Clock,
    ): OperatorRemediationQueueService {
        return OperatorRemediationQueueService(
            paymentRecordRepository = paymentRecordRepository,
            refundOperationsService = refundOperationsService,
            notificationDeliveryRepository = notificationDeliveryRepository,
            notificationDeliveryService = notificationDeliveryService,
            paymentProviderEventRepository = paymentProviderEventRepository,
            paymentWebhookService = paymentWebhookService,
            operatorFailureRecordRepository = operatorFailureRecordRepository,
            auditEventPort = auditEventPort,
            idempotencyStore = idempotencyStore,
            clock = clock,
        )
    }

    private fun resolveOwnerId(configuredOwnerId: String): String {
        if (configuredOwnerId.isNotBlank()) {
            return configuredOwnerId
        }
        val host = runCatching { InetAddress.getLocalHost().hostName }.getOrDefault("unknown-host")
        return "$host-${UUID.randomUUID()}"
    }
}
