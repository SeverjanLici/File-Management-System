package com.docplatform.common.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

data class DepartmentDto(
    val id: UUID,
    val name: String,
    val description: String?,
    val memberCount: Int = 0,
    val createdAt: Instant,
    val updatedAt: Instant
)

data class CreateDepartmentRequest(
    @field:NotBlank(message = "Department name is required")
    @field:Size(min = 2, max = 100, message = "Department name must be between 2 and 100 characters")
    val name: String,

    @field:Size(max = 500, message = "Description must be less than 500 characters")
    val description: String? = null
)

data class UpdateDepartmentRequest(
    @field:Size(min = 2, max = 100, message = "Department name must be between 2 and 100 characters")
    val name: String? = null,

    @field:Size(max = 500, message = "Description must be less than 500 characters")
    val description: String? = null
)

data class DepartmentMemberRequest(
    val userId: UUID
)
