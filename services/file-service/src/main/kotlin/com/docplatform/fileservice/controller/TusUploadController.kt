package com.docplatform.fileservice.controller

import com.docplatform.common.dto.ApiResponse
import com.docplatform.common.dto.UploadCompleteDto
import com.docplatform.common.dto.UploadSessionDto
import com.docplatform.fileservice.service.TusUploadService
import com.docplatform.security.model.AuthenticatedUser
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.*
import java.util.Base64
import java.util.UUID

@RestController
@RequestMapping("/api/v1/upload")
class TusUploadController(
    private val tusUploadService: TusUploadService
) {
    private val logger = LoggerFactory.getLogger(TusUploadController::class.java)

    @RequestMapping(value = ["", "/**"], method = [RequestMethod.POST])
    fun handleTusPost(
        request: HttpServletRequest,
        response: HttpServletResponse,
        @AuthenticationPrincipal jwt: Jwt
    ) {
        handleTusUpload(request, response, jwt)
    }

    @RequestMapping(value = ["/**"], method = [RequestMethod.PATCH])
    fun handleTusPatch(
        request: HttpServletRequest,
        response: HttpServletResponse,
        @AuthenticationPrincipal jwt: Jwt
    ) {
        handleTusUpload(request, response, jwt)
    }

    @RequestMapping(value = ["/**"], method = [RequestMethod.HEAD])
    fun handleTusHead(
        request: HttpServletRequest,
        response: HttpServletResponse,
        @AuthenticationPrincipal jwt: Jwt
    ) {
        handleTusUpload(request, response, jwt)
    }

    @RequestMapping(value = ["/**"], method = [RequestMethod.DELETE])
    fun handleTusDelete(
        request: HttpServletRequest,
        response: HttpServletResponse,
        @AuthenticationPrincipal jwt: Jwt
    ) {
        handleTusUpload(request, response, jwt)
    }

    @RequestMapping(value = ["/**"], method = [RequestMethod.OPTIONS])
    fun handleTusOptions(
        request: HttpServletRequest,
        response: HttpServletResponse,
        @AuthenticationPrincipal jwt: Jwt?
    ) {
        handleTusUpload(request, response, jwt!!)
    }

    private fun handleTusUpload(
        request: HttpServletRequest,
        response: HttpServletResponse,
        @AuthenticationPrincipal jwt: Jwt
    ) {
        val authenticatedUser = AuthenticatedUser.fromJwt(jwt)
        var uploadId: String? = null
        var uploadLength: Long? = null
        var metadata: Map<String, String> = emptyMap()

        // For POST requests (new uploads), read metadata first
        if (request.method == "POST") {
            uploadLength = request.getHeader("Upload-Length")?.toLongOrNull()
            metadata = parseUploadMetadata(request.getHeader("Upload-Metadata"))
        }

        // Process the TUS request (this will set Location header for POST-created uploads)
        tusUploadService.processUpload(request, response)

        // After processing, for POST we should read the Location header set by the TUS service
        if (request.method == "POST") {
            val location = response.getHeader("Location")
            val createdUploadId = location?.substringAfterLast('/')

            if (!createdUploadId.isNullOrBlank()) {
                uploadId = createdUploadId
            }

            val fileName = metadata["filename"] ?: "unnamed"
            val mimeType = metadata["filetype"] ?: "application/octet-stream"

            if (uploadLength != null && uploadId != null) {
                // Persist session using the upload id assigned by the TUS service
                try {
                    tusUploadService.createUploadSession(
                        uploadId = uploadId,
                        fileName = fileName,
                        fileSize = uploadLength,
                        mimeType = mimeType,
                        uploadedBy = UUID.nameUUIDFromBytes(authenticatedUser.externalId.toByteArray())
                    )
                    logger.info("Created upload session: $uploadId for file: $fileName")
                } catch (e: Exception) {
                    logger.error("Failed to create upload session record: $uploadId", e)
                }
            }

            // Handle zero-length uploads created via POST: complete immediately
            if (uploadLength != null && uploadLength == 0L && uploadId != null) {
                try {
                    logger.info("Zero-length upload detected, completing upload: $uploadId")
                    val result = tusUploadService.completeUpload(uploadId)
                    response.setHeader("X-Upload-Complete", "true")
                    response.setHeader("X-File-Id", result.fileId.toString())
                } catch (e: Exception) {
                    logger.error("Failed to complete zero-length upload: $uploadId", e)
                }
            }
        }

        // Check if upload is complete after PATCH requests
        if (request.method == "PATCH") {
            val uploadUri = request.requestURI
            val pathUploadId = uploadUri.substringAfterLast("/")

            val uploadInfo = tusUploadService.getUploadInfo(pathUploadId)
            if (uploadInfo != null && uploadInfo.offset == uploadInfo.length) {
                logger.info("Upload complete, transferring to MinIO: $pathUploadId")
                // Upload complete - transfer to MinIO
                val result = tusUploadService.completeUpload(pathUploadId)

                // Add custom header with file info
                response.setHeader("X-Upload-Complete", "true")
                response.setHeader("X-File-Id", result.fileId.toString())
            }
        }
    }

    @PostMapping("/{uploadId}/complete")
    fun completeUpload(
        @PathVariable uploadId: String
    ): ResponseEntity<ApiResponse<UploadCompleteDto>> {
        val result = tusUploadService.completeUpload(uploadId)
        return ResponseEntity.ok(ApiResponse.success(result))
    }

    @GetMapping("/{uploadId}/status")
    fun getUploadStatus(@PathVariable uploadId: String): ResponseEntity<ApiResponse<UploadSessionDto>> {
        val session = tusUploadService.getUploadSession(uploadId)
        val dto = UploadSessionDto(
            id = session.id!!,
            uploadUrl = "/api/v1/upload/$uploadId",
            fileId = session.id!!,
            offset = session.offset,
            length = session.fileSize,
            expiresAt = session.expiresAt
        )
        return ResponseEntity.ok(ApiResponse.success(dto))
    }

    @DeleteMapping("/{uploadId}")
    fun cancelUpload(@PathVariable uploadId: String): ResponseEntity<Void> {
        tusUploadService.cancelUpload(uploadId)
        return ResponseEntity.noContent().build()
    }

    private fun parseUploadMetadata(metadata: String?): Map<String, String> {
        if (metadata.isNullOrBlank()) return emptyMap()

        return metadata.split(",").mapNotNull { pair ->
            val parts = pair.trim().split(" ", limit = 2)
            if (parts.size == 2) {
                val key = parts[0]
                val value = try {
                    String(Base64.getDecoder().decode(parts[1]))
                } catch (e: Exception) {
                    parts[1]
                }
                key to value
            } else null
        }.toMap()
    }
}
