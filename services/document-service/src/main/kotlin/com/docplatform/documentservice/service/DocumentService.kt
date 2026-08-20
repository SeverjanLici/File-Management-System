package com.docplatform.documentservice.service

import com.docplatform.common.dto.*
import com.docplatform.common.event.DocumentDeletedEvent
import com.docplatform.common.exception.ConflictException
import com.docplatform.common.exception.AccessDeniedException
import com.docplatform.common.exception.ResourceNotFoundException
import com.docplatform.documentservice.entity.AuditAction
import com.docplatform.documentservice.entity.Document
import com.docplatform.documentservice.entity.DocumentAuditLog
import com.docplatform.documentservice.entity.DocumentShare
import com.docplatform.documentservice.entity.DocumentVersion
import com.docplatform.documentservice.messaging.DocumentEventProducer
import com.docplatform.documentservice.repository.DocumentAuditLogRepository
import com.docplatform.documentservice.repository.DocumentRepository
import com.docplatform.documentservice.repository.DocumentShareRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class DocumentService(
    private val documentRepository: DocumentRepository,
    private val documentShareRepository: DocumentShareRepository,
    private val documentAuditLogRepository: DocumentAuditLogRepository,
    private val documentAccessService: DocumentAccessService,
    private val fileCleanupService: FileCleanupService,
    private val documentEventProducer: DocumentEventProducer
) {

    @Transactional
    fun createDocument(
        request: CreateDocumentRequest,
        userId: UUID,
        userName: String
    ): DocumentDto {
        val document = Document(
            name = request.name,
            description = request.description,
            category = request.category,
            tags = request.tags.toNormalizedTagSet(),
            summary = request.summary,
            currentVersionNumber = 1,
            fileId = request.fileId,
            fileName = request.fileName,
            fileSize = request.fileSize,
            mimeType = request.mimeType,
            ownerId = userId,
            ownerName = userName
        )

        document.addVersion(
            DocumentVersion(
                versionNumber = 1,
                fileId = request.fileId,
                fileName = request.fileName,
                fileSize = request.fileSize,
                mimeType = request.mimeType,
                createdBy = userId,
                createdByName = userName
            )
        )

        val savedDocument = documentRepository.save(document)

        logAudit(savedDocument, AuditAction.CREATED, userId, userName, "Document created")

        return savedDocument.toDto(Permission.entries)
    }

    @Transactional(readOnly = true)
    fun getDocument(
        id: UUID,
        userId: UUID,
        userRole: Role,
        departmentIds: List<UUID>
    ): DocumentDto {
        val document = documentRepository.findByIdWithSharesAndVersions(id)
            .orElseThrow { ResourceNotFoundException("Document", id) }

        val permissions = documentAccessService.getGrantedPermissions(document, userId, userRole, departmentIds)

        if (Permission.VIEW !in permissions && document.ownerId != userId && userRole != Role.ADMIN) {
            throw AccessDeniedException("You don't have permission to view this document")
        }

        return document.toDto(permissions)
    }

    @Transactional(readOnly = true)
    fun listDocuments(
        userId: UUID,
        userRole: Role,
        departmentIds: List<UUID>,
        search: String?,
        category: String?,
        tag: String?,
        pageable: Pageable
    ): Page<DocumentSummaryDto> {
        val normalizedSearch = search?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }
        val normalizedCategory = category?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }
        val normalizedTag = tag?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }

        val page = if (userRole == Role.ADMIN) {
            documentRepository.findAllWithSearchAndFilters(
                normalizedSearch,
                normalizedCategory,
                normalizedTag,
                pageable
            )
        } else if (departmentIds.isEmpty()) {
            documentRepository.findAccessibleByUserWithSearchAndFilters(
                userId,
                normalizedSearch,
                normalizedCategory,
                normalizedTag,
                pageable
            )
        } else {
            documentRepository.findAccessibleByUserAndDepartmentsWithSearchAndFilters(
                userId,
                departmentIds,
                normalizedSearch,
                normalizedCategory,
                normalizedTag,
                pageable
            )
        }

        return page.map { it.toSummaryDto() }
    }

    @Transactional(readOnly = true)
    fun listMyDocuments(userId: UUID, pageable: Pageable): Page<DocumentSummaryDto> {
        return documentRepository.findByOwnerId(userId, pageable).map { it.toSummaryDto() }
    }

    @Transactional(readOnly = true)
    fun listDepartmentDocuments(
        departmentId: UUID,
        userRole: Role,
        userDepartmentIds: List<UUID>,
        pageable: Pageable
    ): Page<DocumentSummaryDto> {
        if (userRole != Role.ADMIN && departmentId !in userDepartmentIds) {
            throw AccessDeniedException("You don't have permission to view this department workspace")
        }

        return documentRepository.findByDepartmentShare(departmentId, pageable).map { it.toSummaryDto() }
    }

    @Transactional
    fun updateDocument(
        id: UUID,
        request: UpdateDocumentRequest,
        userId: UUID,
        userName: String,
        userRole: Role,
        departmentIds: List<UUID>
    ): DocumentDto {
        val document = documentRepository.findByIdWithSharesAndVersions(id)
            .orElseThrow { ResourceNotFoundException("Document", id) }

        if (!documentAccessService.hasPermission(document, userId, userRole, departmentIds, Permission.EDIT)) {
            throw AccessDeniedException("You don't have permission to edit this document")
        }

        request.name?.let { document.name = it }
        request.description?.let { document.description = it }
        request.category?.let { document.category = it }
        request.tags?.let { document.tags = it.toNormalizedTagSet() }
        request.summary?.let { document.summary = it }

        val savedDocument = documentRepository.save(document)

        logAudit(savedDocument, AuditAction.UPDATED, userId, userName, "Document updated")

        val permissions = documentAccessService.getGrantedPermissions(document, userId, userRole, departmentIds)
        return savedDocument.toDto(permissions)
    }

    @Transactional
    fun addDocumentVersion(
        id: UUID,
        request: CreateDocumentVersionRequest,
        userId: UUID,
        userName: String,
        userRole: Role,
        departmentIds: List<UUID>
    ): DocumentDto {
        val document = documentRepository.findByIdWithSharesAndVersions(id)
            .orElseThrow { ResourceNotFoundException("Document", id) }

        if (!documentAccessService.hasPermission(document, userId, userRole, departmentIds, Permission.EDIT)) {
            throw AccessDeniedException("You don't have permission to add a version to this document")
        }

        val nextVersion = (document.versions.maxOfOrNull { it.versionNumber } ?: 0) + 1
        document.addVersion(
            DocumentVersion(
                versionNumber = nextVersion,
                fileId = request.fileId,
                fileName = request.fileName,
                fileSize = request.fileSize,
                mimeType = request.mimeType,
                createdBy = userId,
                createdByName = userName
            )
        )

        val savedDocument = documentRepository.save(document)
        logAudit(
            savedDocument,
            AuditAction.VERSION_ADDED,
            userId,
            userName,
            "Added version v$nextVersion (${request.fileName})"
        )

        val permissions = documentAccessService.getGrantedPermissions(savedDocument, userId, userRole, departmentIds)
        return savedDocument.toDto(permissions)
    }

    @Transactional
    fun deleteDocument(
        id: UUID,
        userId: UUID,
        userName: String,
        userRole: Role,
        departmentIds: List<UUID>
    ) {
        val document = documentRepository.findByIdWithSharesAndVersions(id)
            .orElseThrow { ResourceNotFoundException("Document", id) }

        if (!documentAccessService.hasPermission(document, userId, userRole, departmentIds, Permission.DELETE)) {
            throw AccessDeniedException("You don't have permission to delete this document")
        }

        val fileIdsToDelete = document.versions
            .map { it.fileId }
            .toSet()

        fileIdsToDelete.forEach(fileCleanupService::deleteFile)

        documentRepository.delete(document)

        documentEventProducer.publishDocumentDeleted(
            DocumentDeletedEvent(
                documentId = document.id,
                fileId = document.fileId,
                deletedBy = userId
            )
        )
    }


    @Transactional
    fun shareDocument(
        documentId: UUID,
        request: CreateShareRequest,
        userId: UUID,
        userName: String,
        userRole: Role,
        departmentIds: List<UUID>
    ): ShareDto {
        val document = documentRepository.findByIdWithShares(documentId)
            .orElseThrow { ResourceNotFoundException("Document", documentId) }

        if (!documentAccessService.hasPermission(document, userId, userRole, departmentIds, Permission.SHARE)) {
            throw AccessDeniedException("You don't have permission to share this document")
        }

        validateShareRequest(request)
        ensureShareDoesNotAlreadyExist(documentId, request)

        val share = DocumentShare(
            shareType = request.shareType,
            targetId = request.targetId,
            targetName = normalizeTargetName(request),
            permissions = request.permissions.toMutableSet(),
            createdBy = userId
        )

        document.addShare(share)
        documentRepository.save(document)

        logAudit(
            document,
            AuditAction.SHARED,
            userId,
            userName,
            "Shared with ${request.shareType}: ${share.targetName ?: share.targetId ?: "all"}"
        )

        return share.toDto()
    }

    @Transactional
    fun setCompanyWide(
        documentId: UUID,
        companyWide: Boolean,
        userId: UUID,
        userName: String,
        userRole: Role,
        departmentIds: List<UUID>
    ): DocumentDto {
        val document = documentRepository.findByIdWithSharesAndVersions(documentId)
            .orElseThrow { ResourceNotFoundException("Document", documentId) }

        if (!documentAccessService.hasPermission(document, userId, userRole, departmentIds, Permission.SHARE)) {
            throw AccessDeniedException("You don't have permission to modify sharing for this document")
        }

        document.companyWide = companyWide
        val savedDocument = documentRepository.save(document)

        logAudit(
            document,
            AuditAction.PERMISSION_CHANGED,
            userId,
            userName,
            "Company-wide sharing ${if (companyWide) "enabled" else "disabled"}"
        )

        val permissions = documentAccessService.getGrantedPermissions(document, userId, userRole, departmentIds)
        return savedDocument.toDto(permissions)
    }

    @Transactional(readOnly = true)
    fun getDocumentShares(documentId: UUID): List<ShareDto> {
        val document = documentRepository.findByIdWithShares(documentId)
            .orElseThrow { ResourceNotFoundException("Document", documentId) }
        return document.shares.map { it.toDto() }
    }

    @Transactional
    fun removeShare(
        documentId: UUID,
        shareId: UUID,
        userId: UUID,
        userName: String,
        userRole: Role,
        departmentIds: List<UUID>
    ) {
        val document = documentRepository.findByIdWithShares(documentId)
            .orElseThrow { ResourceNotFoundException("Document", documentId) }

        if (!documentAccessService.hasPermission(document, userId, userRole, departmentIds, Permission.SHARE)) {
            throw AccessDeniedException("You don't have permission to modify sharing for this document")
        }

        val share = document.shares.find { it.id == shareId }
            ?: throw ResourceNotFoundException("Share", shareId)

        document.removeShare(share)
        documentRepository.save(document)

        logAudit(document, AuditAction.UNSHARED, userId, userName, "Removed share: $shareId")
    }

    @Transactional
    fun removeDepartmentShares(departmentId: UUID): Long {
        return documentShareRepository.deleteByShareTypeAndTargetId(ShareType.DEPARTMENT, departmentId)
    }

    @Transactional
    fun syncAiMetadataByFileId(fileId: UUID, request: SyncDocumentAiMetadataRequest): Boolean {
        val document = documentRepository.findByFileId(fileId) ?: return false

        request.summary
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.let { document.summary = it }

        request.category
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.let { document.category = it }

        if (request.tags.isNotEmpty()) {
            val mergedTags = (document.tags + request.tags.toNormalizedTagSet()).toMutableSet()
            document.tags = mergedTags
        }

        documentRepository.save(document)
        return true
    }

    private fun logAudit(document: Document, action: AuditAction, userId: UUID, userName: String, details: String) {
        val auditLog = DocumentAuditLog(
            action = action,
            userId = userId,
            userName = userName,
            details = details
        )
        auditLog.document = document
        document.auditLogs.add(auditLog)
    }

    private fun Document.toDto(permissions: List<Permission>): DocumentDto {
        return DocumentDto(
            id = this.id!!,
            name = this.name,
            description = this.description,
            category = this.category,
            tags = this.tags.sorted(),
            summary = this.summary,
            currentVersionNumber = this.currentVersionNumber,
            fileId = this.fileId,
            fileName = this.fileName,
            fileSize = this.fileSize,
            mimeType = this.mimeType,
            ownerId = this.ownerId,
            ownerName = this.ownerName,
            permissions = permissions,
            shares = this.shares.map { it.toDto() },
            versions = this.versions.sortedByDescending { it.versionNumber }.map { it.toDto() },
            createdAt = this.createdAt,
            updatedAt = this.updatedAt
        )
    }

    private fun Document.toSummaryDto(): DocumentSummaryDto {
        return DocumentSummaryDto(
            id = this.id!!,
            name = this.name,
            category = this.category,
            tags = this.tags.sorted(),
            summary = this.summary,
            currentVersionNumber = this.currentVersionNumber,
            fileId = this.fileId,
            fileName = this.fileName,
            fileSize = this.fileSize,
            mimeType = this.mimeType,
            ownerId = this.ownerId,
            ownerName = this.ownerName,
            createdAt = this.createdAt
        )
    }

    private fun DocumentShare.toDto(): ShareDto {
        return ShareDto(
            id = this.id!!,
            documentId = this.document?.id!!,
            shareType = this.shareType,
            targetId = this.targetId,
            targetName = this.targetName,
            permissions = this.permissions.toList(),
            createdAt = this.createdAt,
            createdBy = this.createdBy
        )
    }

    private fun DocumentVersion.toDto(): DocumentVersionDto {
        return DocumentVersionDto(
            id = this.id!!,
            versionNumber = this.versionNumber,
            fileId = this.fileId,
            fileName = this.fileName,
            fileSize = this.fileSize,
            mimeType = this.mimeType,
            createdBy = this.createdBy,
            createdByName = this.createdByName,
            createdAt = this.createdAt
        )
    }

    private fun List<String>.toNormalizedTagSet(): MutableSet<String> {
        return this
            .map { it.trim() }
            .map { it.lowercase() }
            .filter { it.isNotEmpty() }
            .toCollection(linkedSetOf())
            .toMutableSet()
    }

    private fun validateShareRequest(request: CreateShareRequest) {
        when (request.shareType) {
            ShareType.USER,
            ShareType.DEPARTMENT -> {
                if (request.targetId == null) {
                    throw IllegalArgumentException("targetId is required for ${request.shareType} shares")
                }
            }

            ShareType.ROLE -> {
                throw IllegalArgumentException("Role-based sharing is not supported")
            }

            ShareType.COMPANY -> {
                // no-op
            }
        }
    }

    private fun ensureShareDoesNotAlreadyExist(documentId: UUID, request: CreateShareRequest) {
        val duplicateExists = when (request.shareType) {
            ShareType.USER,
            ShareType.DEPARTMENT -> {
                val targetId = request.targetId ?: return
                documentShareRepository.findByDocumentIdAndShareTypeAndTargetId(
                    documentId,
                    request.shareType,
                    targetId
                ) != null
            }

            ShareType.ROLE -> false

            ShareType.COMPANY -> {
                documentShareRepository.findByDocumentIdAndShareType(documentId, ShareType.COMPANY).isNotEmpty()
            }
        }

        if (duplicateExists) {
            throw ConflictException("A share for this target already exists")
        }
    }

    private fun normalizeTargetName(request: CreateShareRequest): String? {
        return when (request.shareType) {
            ShareType.ROLE -> request.targetName?.trim()?.uppercase()
            ShareType.COMPANY -> "Everyone"
            else -> request.targetName?.trim()?.takeIf { it.isNotEmpty() }
        }
    }
}
