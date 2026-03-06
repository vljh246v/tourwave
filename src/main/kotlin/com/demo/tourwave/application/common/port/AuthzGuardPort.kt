package com.demo.tourwave.application.common.port

interface AuthzGuardPort {
    fun requireActorUserId(actorUserId: Long?): Long
}

