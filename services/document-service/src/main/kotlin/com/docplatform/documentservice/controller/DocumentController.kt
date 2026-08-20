package com.docplatform.documentservice.controller

import com.docplatform.common.dto.*
import com.docplatform.documentservice.service.DocumentService
import com.docplatform.documentservice.service.UserContextService
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/documents")
class DocumentController(
    private val documentService: DocumentService,
    private val userContextService: UserContextService
) {

    @PostMapping
    fun createDocument(
        @Valid @RequestBody request: CreateDocumentRequest,
        @AuthenticationPrincipal jwt: Jwt
    ): ResponseEntity<ApiResponse<DocumentDto>> {
        val context = userContextService.getUserContext(jwt)
        val document = documentService.createDocument(request, context.userId, context.userName)
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success(document))
    }

    @GetMapping("/{id}")
    fun getDocument(
        @PathVariable id: UUID,
        @AuthenticationPrincipal jwt: Jwt
    ): ResponseEntity<ApiResponse<DocumentDto>> {
        val context = userContextService.getUserContext(jwt)
        val document = documentService.getDocument(id, context.userId, context.role, context.departmentIds)
        return ResponseEntity.ok(ApiResponse.success(document))
    }

    @GetMapping
    fun listDocuments(
        @RequestParam(required = false) search: String?,
        @RequestParam(required = false) category: String?,
        @RequestParam(required = false) tag: String?,
        @PageableDefault(size = 20) pageable: Pageable,
        @AuthenticationPrincipal jwt: Jwt
    ): ResponseEntity<ApiResponse<PagedResponse<DocumentSummaryDto>>> {
        val context = userContextService.getUserContext(jwt)
        val page = documentService.listDocuments(
            context.userId,
            context.role,
            context.departmentIds,
            search,
            category,
            tag,
            pageable
        )
        val pagedResponse = PagedResponse(
            content = page.content,
            page = page.number,
            size = page.size,
            totalElements = page.totalElements,
            totalPages = page.totalPages,
            hasNext = page.hasNext(),
            hasPrevious = page.hasPrevious()
        )
        return ResponseEntity.ok(ApiResponse.success(pagedResponse))
    }

    @GetMapping("/my")
    fun listMyDocuments(
        @PageableDefault(size = 20) pageable: Pageable,
        @AuthenticationPrincipal jwt: Jwt
    ): ResponseEntity<ApiResponse<PagedResponse<DocumentSummaryDto>>> {
        val context = userContextService.getUserContext(jwt)
        val page = documentService.listMyDocuments(context.userId, pageable)
        val pagedResponse = PagedResponse(
            content = page.content,
            page = page.number,
            size = page.size,
            totalElements = page.totalElements,
            totalPages = page.totalPages,
            hasNext = page.hasNext(),
            hasPrevious = page.hasPrevious()
        )
        return ResponseEntity.ok(ApiResponse.success(pagedResponse))
    }

    @GetMapping("/departments/{departmentId}")
    fun listDepartmentDocuments(
        @PathVariable departmentId: UUID,
        @PageableDefault(size = 20) pageable: Pageable,
        @AuthenticationPrincipal jwt: Jwt
    ): ResponseEntity<ApiResponse<PagedResponse<DocumentSummaryDto>>> {
        val context = userContextService.getUserContext(jwt)
        val page = documentService.listDepartmentDocuments(
            departmentId,
            context.role,
            context.departmentIds,
            pageable
        )
        val pagedResponse = PagedResponse(
            content = page.content,
            page = page.number,
            size = page.size,
            totalElements = page.totalElements,
            totalPages = page.totalPages,
            hasNext = page.hasNext(),
            hasPrevious = page.hasPrevious()
        )
        return ResponseEntity.ok(ApiResponse.success(pagedResponse))
    }

    @PutMapping("/{id}")
    fun updateDocument(
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateDocumentRequest,
        @AuthenticationPrincipal jwt: Jwt
    ): ResponseEntity<ApiResponse<DocumentDto>> {
        val context = userContextService.getUserContext(jwt)
        val document = documentService.updateDocument(
            id, request, context.userId, context.userName, context.role, context.departmentIds
        )
        return ResponseEntity.ok(ApiResponse.success(document))
    }

    @PostMapping("/{id}/versions")
    fun addDocumentVersion(
        @PathVariable id: UUID,
        @Valid @RequestBody request: CreateDocumentVersionRequest,
        @AuthenticationPrincipal jwt: Jwt
    ): ResponseEntity<ApiResponse<DocumentDto>> {
        val context = userContextService.getUserContext(jwt)
        val document = documentService.addDocumentVersion(
            id,
            request,
            context.userId,
            context.userName,
            context.role,
            context.departmentIds
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(document))
    }

    @DeleteMapping("/{id}")
    fun deleteDocument(
        @PathVariable id: UUID,
        @AuthenticationPrincipal jwt: Jwt
    ): ResponseEntity<Void> {
        val context = userContextService.getUserContext(jwt)
        documentService.deleteDocument(id, context.userId, context.userName, context.role, context.departmentIds)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/{id}/share")
    fun shareDocument(
        @PathVariable id: UUID,
        @Valid @RequestBody request: CreateShareRequest,
        @AuthenticationPrincipal jwt: Jwt
    ): ResponseEntity<ApiResponse<ShareDto>> {
        val context = userContextService.getUserContext(jwt)
        val share = documentService.shareDocument(
            id, request, context.userId, context.userName, context.role, context.departmentIds
        )
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success(share))
    }

    @PostMapping("/{id}/company-wide")
    fun setCompanyWide(
        @PathVariable id: UUID,
        @RequestParam enabled: Boolean,
        @AuthenticationPrincipal jwt: Jwt
    ): ResponseEntity<ApiResponse<DocumentDto>> {
        val context = userContextService.getUserContext(jwt)
        val document = documentService.setCompanyWide(
            id, enabled, context.userId, context.userName, context.role, context.departmentIds
        )
        return ResponseEntity.ok(ApiResponse.success(document))
    }

    @GetMapping("/{id}/shares")
    fun getDocumentShares(@PathVariable id: UUID): ResponseEntity<ApiResponse<List<ShareDto>>> {
        val shares = documentService.getDocumentShares(id)
        return ResponseEntity.ok(ApiResponse.success(shares))
    }

    @DeleteMapping("/{id}/shares/{shareId}")
    fun removeShare(
        @PathVariable id: UUID,
        @PathVariable shareId: UUID,
        @AuthenticationPrincipal jwt: Jwt
    ): ResponseEntity<Void> {
        val context = userContextService.getUserContext(jwt)
        documentService.removeShare(
            id, shareId, context.userId, context.userName, context.role, context.departmentIds
        )
        return ResponseEntity.noContent().build()
    }
}
