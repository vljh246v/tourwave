package com.demo.tourwave.repository

import org.springframework.stereotype.Component
import com.demo.tourwave.domain.instructor.Instructor
import com.demo.tourwave.domain.instructor.port.InstructorQueryPort

@Component
class InstructorPersistent: InstructorQueryPort {
    override fun getInstructorByEmail(email: String): Instructor? {
        TODO("Not yet implemented")
    }
}
