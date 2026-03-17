package com.demo.tourwave.adapter.out.persistence.jpa.auth

import org.springframework.data.jpa.repository.JpaRepository

interface UserActionTokenJpaRepository : JpaRepository<UserActionTokenJpaEntity, Long>
