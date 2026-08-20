package com.docplatform.documentservice.service

import com.docplatform.common.dto.*
import com.docplatform.documentservice.entity.Document
import com.docplatform.documentservice.repository.DocumentRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class DocumentAccessService(
    private val documentRepository: DocumentRepository
) {

    fun checkAccess(
        documentId: UUID,
        userId: UUID,
        userRole: Role,
        departmentIds: List<UUID>,
        requiredPermission: Permission
    ): AccessCheckResponse {
        val document = documentRepository.findByIdWithShares(documentId).orElse(null)
            ?: return AccessCheckResponse(
                hasAccess = false,
                reason = "Document not found"
            )

        // Admin has full access
        if (userRole == Role.ADMIN) {
            return AccessCheckResponse(
                hasAccess = true,
                grantedPermissions = Permission.entries,
                reason = "Admin access"
            )
        }

        // Owner has full access
        if (document.ownerId == userId) {
            return AccessCheckResponse(
                hasAccess = true,
                grantedPermissions = Permission.entries,
                reason = "Owner access"
            )
        }

        if (hasManagerDepartmentAccess(document, userRole, departmentIds)) {
            return AccessCheckResponse(
                hasAccess = true,
                grantedPermissions = Permission.entries,
                reason = "Manager department access"
            )
        }

        val grantedPermissions = mutableSetOf<Permission>()

        // Check company-wide sharing
        if (document.companyWide) {
            grantedPermissions.add(Permission.VIEW)
        }

        // Check user-specific shares
        document.shares
            .filter { it.shareType == ShareType.USER && it.targetId == userId }
            .forEach { grantedPermissions.addAll(it.permissions) }

        // Check department shares
        document.shares
            .filter { it.shareType == ShareType.DEPARTMENT && it.targetId in departmentIds }
            .forEach { grantedPermissions.addAll(it.permissions) }

        // Check company shares
        document.shares
            .filter { it.shareType == ShareType.COMPANY }
            .forEach { grantedPermissions.addAll(it.permissions) }

        val hasAccess = requiredPermission in grantedPermissions
        return AccessCheckResponse(
            hasAccess = hasAccess,
            grantedPermissions = grantedPermissions.toList(),
            reason = if (hasAccess) "Permission granted via sharing" else "Insufficient permissions"
        )
    }

    fun getGrantedPermissions(
        document: Document,
        userId: UUID,
        userRole: Role,
        departmentIds: List<UUID>
    ): List<Permission> {
        // Admin has full access
        if (userRole == Role.ADMIN) {
            return Permission.entries
        }

        // Owner has full access
        if (document.ownerId == userId) {
            return Permission.entries
        }

        if (hasManagerDepartmentAccess(document, userRole, departmentIds)) {
            return Permission.entries
        }

        val grantedPermissions = mutableSetOf<Permission>()

        // Check company-wide sharing
        if (document.companyWide) {
            grantedPermissions.add(Permission.VIEW)
        }

        // Check user-specific shares
        document.shares
            .filter { it.shareType == ShareType.USER && it.targetId == userId }
            .forEach { grantedPermissions.addAll(it.permissions) }

        // Check department shares
        document.shares
            .filter { it.shareType == ShareType.DEPARTMENT && it.targetId in departmentIds }
            .forEach { grantedPermissions.addAll(it.permissions) }

        // Check company shares
        document.shares
            .filter { it.shareType == ShareType.COMPANY }
            .forEach { grantedPermissions.addAll(it.permissions) }

        return grantedPermissions.toList()
    }

    fun hasPermission(
        document: Document,
        userId: UUID,
        userRole: Role,
        departmentIds: List<UUID>,
        requiredPermission: Permission
    ): Boolean {
        val permissions = getGrantedPermissions(document, userId, userRole, departmentIds)
        return requiredPermission in permissions
    }

    private fun hasManagerDepartmentAccess(
        document: Document,
        userRole: Role,
        departmentIds: List<UUID>
    ): Boolean {
        if (userRole != Role.MANAGER || departmentIds.isEmpty()) {
            return false
        }

        return document.shares.any {
            it.shareType == ShareType.DEPARTMENT && it.targetId in departmentIds
        }
    }
}
