package com.docplatform.aiservice.controller

import com.docplatform.aiservice.dto.EditProcessingRequest
import com.docplatform.aiservice.dto.ProcessingResultDto
import com.docplatform.aiservice.dto.ProcessingStatusDto
import com.docplatform.aiservice.entity.ProcessingStatus
import com.docplatform.aiservice.entity.DocumentProcessing
import com.docplatform.aiservice.service.DocumentProcessingService
import com.docplatform.common.dto.ApiResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken


@RestController
@RequestMapping("/api/v1/processing")
class ProcessingController(
    private val documentProcessingService: DocumentProcessingService
) {

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    fun getAll(): ResponseEntity<List<DocumentProcessing>> {

        return ResponseEntity.ok(documentProcessingService.getAllProcessing())
    }



    @GetMapping("/status/file/{fileId}")
    fun getProcessingStatus(@PathVariable fileId: UUID): ResponseEntity<ApiResponse<ProcessingStatusDto>> {
        val status = documentProcessingService.getProcessingStatus(fileId)
        return if (status != null) ResponseEntity.ok(ApiResponse.success(status)) else ResponseEntity.notFound().build()
    }

    @GetMapping("/result/file/{fileId}")
    fun getProcessingResult(@PathVariable fileId: UUID): ResponseEntity<ApiResponse<ProcessingResultDto>> {
        val result = documentProcessingService.getProcessingResult(fileId)
        return if (result != null) ResponseEntity.ok(ApiResponse.success(result)) else ResponseEntity.notFound().build()
    }

    @PostMapping("/retry/{processingId}")
    fun retryProcessing(@PathVariable processingId: UUID): ResponseEntity<String> {
        return try {
            documentProcessingService.retryProcessing(processingId)
            ResponseEntity.ok("Processing retry initiated")
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(e.message)
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/metrics")
    fun getMetrics(): ResponseEntity<Map<String, Any>> {
        return ResponseEntity.ok(documentProcessingService.getMetrics())
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/override/{processingId}")
    fun overrideMetadata(
        @PathVariable processingId: UUID,
        @RequestBody metadata: String
    ): ResponseEntity<String> {
        documentProcessingService.overrideMetadata(processingId, metadata)
        return ResponseEntity.ok("Metadata overridden")
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/validate/{processingId}")
    fun validate(
        @PathVariable processingId: UUID,
        authentication: JwtAuthenticationToken
    ): ResponseEntity<String> {
        val jwt = authentication.token

        val name = jwt.getClaimAsString("given_name")
        val surname = jwt.getClaimAsString("family_name")

        val fullName = "$name $surname"

        documentProcessingService.validateProcessing(processingId, fullName)
        return ResponseEntity.ok("Validated")
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/edit/{id}")
    fun editProcessing(
        @PathVariable id: UUID,
        @RequestBody request: EditProcessingRequest,
        authentication: JwtAuthenticationToken
    ) {
        val jwt = authentication.token

        val name = jwt.getClaimAsString("given_name") ?: ""
        val surname = jwt.getClaimAsString("family_name") ?: ""

        val fullName = "$name $surname".trim()

        documentProcessingService.editProcessing(id, request, fullName)
    }
}
