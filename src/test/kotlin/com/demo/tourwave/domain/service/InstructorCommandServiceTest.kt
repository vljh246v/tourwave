package com.demo.tourwave.domain.service

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever
import com.demo.tourwave.domain.instructor.Instructor
import com.demo.tourwave.domain.instructor.port.InstructorQueryPort
import com.demo.tourwave.domain.instructor.service.InstructorCommandService

class InstructorCommandServiceTest {

    private val instructorQueryPort: InstructorQueryPort = mock()
    private val instructorCommandService = InstructorCommandService(instructorQueryPort)

    @Test
    fun `registerInstructor should throw exception when instructor with email already exists`() {
        val existingEmail = "old@test.com"
        whenever(instructorQueryPort.getInstructorByEmail(existingEmail)).thenReturn(Instructor.create("old instructor", existingEmail))

        val exception = assertThrows<IllegalArgumentException> {
            instructorCommandService.registerInstructor("new instructor", existingEmail)
        }
        assertEquals("Instructor with email $existingEmail already exists", exception.message)
    }

    @Test
    fun `registerInstructor should create new instructor when email does not exist`() {
        val newEmail = "new@test.com"
        whenever(instructorQueryPort.getInstructorByEmail(newEmail)).thenReturn(null)

        val instructor = instructorCommandService.registerInstructor("new instructor", newEmail)

        assertNotNull(instructor)
        assertEquals("new instructor", instructor.name)
        assertEquals(newEmail, instructor.email)
    }
}
