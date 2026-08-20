package com.docplatform.common.dto

import java.util.UUID

data class AccessCheckRequest(
    val userId: UUID,
    val documentId: UUID,
    val requiredPermission: Permission
)

data class AccessCheckResponse(
    val hasAccess: Boolean,
    val grantedPermissions: List<Permission> = emptyList(),
    val reason: String? = null
)

data class UserAccessContext(
    val userId: UUID,
    val role: Role,
    val departmentIds: List<UUID>
)
