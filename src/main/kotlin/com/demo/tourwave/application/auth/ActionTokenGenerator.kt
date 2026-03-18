package com.demo.tourwave.application.auth

fun interface ActionTokenGenerator {
    fun generate(): String
}
