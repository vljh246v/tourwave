package com.demo.tourwave.domain.organization

enum class OrganizationRole {
    MEMBER,
    ADMIN,
    OWNER;

    fun canManageMembers(): Boolean = this == ADMIN || this == OWNER
}
