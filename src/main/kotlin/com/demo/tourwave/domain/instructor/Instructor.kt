package com.demo.tourwave.domain.instructor

class Instructor(
    val name: String,
    val email: String
) {
    companion object {
        fun create(name: String, email: String): Instructor {
            return Instructor(
                name = name,
                email = email
            )
        }
    }
}