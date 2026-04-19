package com.demo.tourwave.adapter.out.persistence.jpa.operations

import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface WorkerJobLockJpaRepository : JpaRepository<WorkerJobLockJpaEntity, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select lock from WorkerJobLockJpaEntity lock where lock.lockName = :lockName")
    fun findLockedByLockName(
        @Param("lockName") lockName: String,
    ): WorkerJobLockJpaEntity?
}
