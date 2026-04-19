package com.demo.tourwave.domain.organization

import java.time.Instant

data class Organization(
    val id: Long? = null,
    val slug: String,
    val name: String,
    val description: String? = null,
    val publicDescription: String? = null,
    val contactEmail: String? = null,
    val contactPhone: String? = null,
    val websiteUrl: String? = null,
    val businessName: String? = null,
    val businessRegistrationNumber: String? = null,
    val attachmentAssetIds: List<Long> = emptyList(),
    val timezone: String,
    val status: OrganizationStatus = OrganizationStatus.ACTIVE,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    fun updateProfile(
        name: String,
        description: String?,
        publicDescription: String?,
        contactEmail: String?,
        contactPhone: String?,
        websiteUrl: String?,
        businessName: String?,
        businessRegistrationNumber: String?,
        timezone: String,
        now: Instant,
    ): Organization {
        return copy(
            name = name,
            description = description,
            publicDescription = publicDescription,
            contactEmail = contactEmail,
            contactPhone = contactPhone,
            websiteUrl = websiteUrl,
            businessName = businessName,
            businessRegistrationNumber = businessRegistrationNumber,
            timezone = timezone,
            updatedAt = now,
        )
    }

    fun updateAttachments(
        assetIds: List<Long>,
        now: Instant,
    ): Organization {
        return copy(
            attachmentAssetIds = assetIds,
            updatedAt = now,
        )
    }

    companion object {
        fun create(
            slug: String,
            name: String,
            description: String?,
            publicDescription: String?,
            contactEmail: String?,
            contactPhone: String?,
            websiteUrl: String?,
            businessName: String?,
            businessRegistrationNumber: String?,
            timezone: String,
            now: Instant,
        ): Organization {
            return Organization(
                slug = slug,
                name = name,
                description = description,
                publicDescription = publicDescription,
                contactEmail = contactEmail,
                contactPhone = contactPhone,
                websiteUrl = websiteUrl,
                businessName = businessName,
                businessRegistrationNumber = businessRegistrationNumber,
                timezone = timezone,
                createdAt = now,
                updatedAt = now,
            )
        }
    }
}
