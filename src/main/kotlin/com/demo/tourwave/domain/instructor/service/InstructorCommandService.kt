package com.demo.tourwave.domain.instructor.service

import org.springframework.stereotype.Service
import com.demo.tourwave.domain.instructor.Instructor
import com.demo.tourwave.domain.instructor.handler.InstructorCommandHandler
import com.demo.tourwave.domain.instructor.port.InstructorQueryPort

@Service
class InstructorCommandService(
    private val instructorQueryPort: InstructorQueryPort
): InstructorCommandHandler {
    override fun registerInstructor(name: String, email: String): Instructor {
        if (instructorQueryPort.getInstructorByEmail(email) != null) {
            throw IllegalArgumentException("Instructor with email $email already exists")
        }
        return Instructor.create(name, email)
    }
}
