package com.demo.tourwave.domain.instructor.port

import com.demo.tourwave.domain.instructor.Instructor

interface InstructorQueryPort {
    fun getInstructorByEmail(email: String): Instructor?
}
