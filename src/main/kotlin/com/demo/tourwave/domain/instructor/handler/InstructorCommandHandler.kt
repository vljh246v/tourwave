package com.demo.tourwave.domain.instructor.handler

import com.demo.tourwave.domain.instructor.Instructor

interface InstructorCommandHandler {
    fun registerInstructor(name: String, email: String): Instructor
}