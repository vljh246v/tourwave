package com.demo.tourwave.adapter.out.persistence.jpa.asset

import org.springframework.data.jpa.repository.JpaRepository

interface AssetJpaRepository : JpaRepository<AssetJpaEntity, Long>
