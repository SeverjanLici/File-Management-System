package com.docplatform.common.dto

import java.time.Instant
import java.util.UUID

data class UserDto(
    val id: UUID,
    val externalId: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val role: Role,
    val departments: List<DepartmentDto> = emptyList(),
    val createdAt: Instant,
    val updatedAt: Instant
)

data class UserSummaryDto(
    val id: UUID,
    val email: String,
    val firstName: String,
    val lastName: String,
    val role: Role
)

data class UpdateUserRequest(
    val firstName: String? = null,
    val lastName: String? = null,
    val role: Role? = null
)

enum class Role {
    ADMIN,
    MANAGER,
    USER
}
