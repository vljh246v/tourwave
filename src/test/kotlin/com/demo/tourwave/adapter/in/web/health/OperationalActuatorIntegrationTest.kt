package com.demo.tourwave.adapter.`in`.web.health

import com.demo.tourwave.application.common.ScheduledJobCoordinator
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
class OperationalActuatorIntegrationTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var scheduledJobCoordinator: ScheduledJobCoordinator

    @BeforeEach
    fun setUp() {
        scheduledJobCoordinator.run(
            jobName = "actuator-smoke",
            onSkipped = { Unit }
        ) {
            Unit
        }
    }

    @Test
    fun `health endpoints expose worker readiness components`() {
        mockMvc.perform(get("/actuator/health"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.components.workerJobLocks.status").value("UP"))
            .andExpect(jsonPath("$.components.workerJobs.status").exists())

        mockMvc.perform(get("/actuator/health/liveness"))
            .andExpect(status().isOk)

        mockMvc.perform(get("/actuator/health/readiness"))
            .andExpect(status().isOk)
    }

    @Test
    fun `metrics endpoint exposes custom job metrics`() {
        mockMvc.perform(get("/actuator/metrics/tourwave.job.execution"))
            .andExpect(status().isOk)
            .andExpect(content().string(org.hamcrest.Matchers.containsString("tourwave.job.execution")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("actuator-smoke")))
    }
}
