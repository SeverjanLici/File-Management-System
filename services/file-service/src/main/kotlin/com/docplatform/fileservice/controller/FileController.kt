package com.docplatform.fileservice.controller

import com.docplatform.common.dto.ApiResponse
import com.docplatform.common.dto.FileDto
import com.docplatform.common.dto.PagedResponse
import com.docplatform.common.dto.PresignedUrlDto
import com.docplatform.fileservice.service.FileService
import com.docplatform.security.model.AuthenticatedUser
import org.springframework.core.io.InputStreamResource
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/files")
class FileController(
    private val fileService: FileService
) {

    @GetMapping("/{id}")
    fun getFile(@PathVariable id: UUID): ResponseEntity<ApiResponse<FileDto>> {
        val file = fileService.getFile(id)
        return ResponseEntity.ok(ApiResponse.success(file))
    }

    @GetMapping("/{id}/download")
    fun downloadFile(@PathVariable id: UUID): ResponseEntity<InputStreamResource> {
        return fileService.downloadFile(id)
    }

    @GetMapping("/{id}/presigned")
    fun getPresignedUrl(
        @PathVariable id: UUID,
        @RequestParam(defaultValue = "60") expirationMinutes: Int
    ): ResponseEntity<ApiResponse<PresignedUrlDto>> {
        val presignedUrl = fileService.getPresignedUrl(id, expirationMinutes)
        return ResponseEntity.ok(ApiResponse.success(presignedUrl))
    }

    @GetMapping("/{id}/preview")
    fun getPreviewUrl(
        @PathVariable id: UUID,
        @RequestParam(defaultValue = "60") expirationMinutes: Int
    ): ResponseEntity<ApiResponse<PresignedUrlDto>> {
        val presignedUrl = fileService.getPreviewUrl(id, expirationMinutes)
        return ResponseEntity.ok(ApiResponse.success(presignedUrl))
    }

    @GetMapping("/my")
    fun listMyFiles(
        @PageableDefault(size = 20) pageable: Pageable,
        @AuthenticationPrincipal jwt: Jwt
    ): ResponseEntity<ApiResponse<PagedResponse<FileDto>>> {
        val authenticatedUser = AuthenticatedUser.fromJwt(jwt)
        val userId = UUID.nameUUIDFromBytes(authenticatedUser.externalId.toByteArray())
        val page = fileService.listUserFiles(userId, pageable)

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

    @DeleteMapping("/{id}")
    fun deleteFile(@PathVariable id: UUID): ResponseEntity<Void> {
        fileService.deleteFile(id)
        return ResponseEntity.noContent().build()
    }
}
