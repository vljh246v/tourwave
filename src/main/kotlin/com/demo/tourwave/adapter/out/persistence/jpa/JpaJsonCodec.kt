package com.demo.tourwave.adapter.out.persistence.jpa

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

internal object JpaJsonCodec {
    private val objectMapper = jacksonObjectMapper()

    fun writeList(values: List<String>): String = objectMapper.writeValueAsString(values)

    fun writeLongList(values: List<Long>): String = objectMapper.writeValueAsString(values)

    fun readList(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()
        return objectMapper.readValue(raw, objectMapper.typeFactory.constructCollectionType(List::class.java, String::class.java))
    }

    fun readLongList(raw: String?): List<Long> {
        if (raw.isNullOrBlank()) return emptyList()
        return objectMapper.readValue(raw, objectMapper.typeFactory.constructCollectionType(List::class.java, java.lang.Long::class.java))
    }
}
