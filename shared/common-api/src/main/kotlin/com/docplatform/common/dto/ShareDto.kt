package com.docplatform.common.dto

import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

data class ShareDto(
    val id: UUID,
    val documentId: UUID,
    val shareType: ShareType,
    val targetId: UUID?,
    val targetName: String?,
    val permissions: List<Permission>,
    val createdAt: Instant,
    val createdBy: UUID
)

data class CreateShareRequest(
    val shareType: ShareType,
    val targetId: UUID? = null,
    @field:Size(max = 120, message = "Target name must be less than 120 characters")
    val targetName: String? = null,

    @field:NotEmpty(message = "At least one permission is required")
    val permissions: List<Permission>
)

data class ShareWithUsersRequest(
    @field:NotEmpty(message = "At least one user ID is required")
    val userIds: List<UUID>,

    @field:NotEmpty(message = "At least one permission is required")
    val permissions: List<Permission>
)

data class ShareWithDepartmentsRequest(
    @field:NotEmpty(message = "At least one department ID is required")
    val departmentIds: List<UUID>,

    @field:NotEmpty(message = "At least one permission is required")
    val permissions: List<Permission>
)

data class ShareWithRoleRequest(
    val role: Role,

    @field:NotEmpty(message = "At least one permission is required")
    val permissions: List<Permission>
)

enum class ShareType {
    USER,
    DEPARTMENT,
    ROLE,
    COMPANY
}
