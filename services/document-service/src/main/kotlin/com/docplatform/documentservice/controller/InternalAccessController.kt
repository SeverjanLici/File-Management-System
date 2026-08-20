package com.docplatform.documentservice.controller

import com.docplatform.common.dto.AccessCheckRequest
import com.docplatform.common.dto.AccessCheckResponse
import com.docplatform.common.dto.ApiResponse
import com.docplatform.common.dto.Role
import com.docplatform.common.dto.SyncDocumentAiMetadataRequest
import com.docplatform.documentservice.service.DocumentAccessService
import com.docplatform.documentservice.service.DocumentService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/internal")
class InternalAccessController(
    private val documentAccessService: DocumentAccessService,
    private val documentService: DocumentService
) {

    @PostMapping("/access-check")
    fun checkAccess(
        @RequestBody request: AccessCheckRequest,
        @RequestParam userRole: Role,
        @RequestParam departmentIds: List<UUID>
    ): ResponseEntity<ApiResponse<AccessCheckResponse>> {
        val response = documentAccessService.checkAccess(
            documentId = request.documentId,
            userId = request.userId,
            userRole = userRole,
            departmentIds = departmentIds,
            requiredPermission = request.requiredPermission
        )
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @DeleteMapping("/departments/{departmentId}/shares")
    fun removeDepartmentShares(@PathVariable departmentId: UUID): ResponseEntity<ApiResponse<Long>> {
        val deletedShares = documentService.removeDepartmentShares(departmentId)
        return ResponseEntity.ok(ApiResponse.success(deletedShares))
    }

    @PostMapping("/documents/metadata/file/{fileId}")
    fun syncDocumentMetadataByFileId(
        @PathVariable fileId: UUID,
        @RequestBody request: SyncDocumentAiMetadataRequest
    ): ResponseEntity<ApiResponse<Boolean>> {
        val updated = documentService.syncAiMetadataByFileId(fileId, request)
        return ResponseEntity.ok(ApiResponse.success(updated))
    }
}
