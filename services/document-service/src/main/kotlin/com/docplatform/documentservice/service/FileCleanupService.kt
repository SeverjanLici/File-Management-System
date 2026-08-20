package com.docplatform.documentservice.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import java.util.UUID

@Service
class FileCleanupService(
    @Value("\${services.file-service.url:http://localhost:8083}")
    private val fileServiceUrl: String
) {
    private val restClient = RestClient.builder()
        .baseUrl(fileServiceUrl)
        .build()

    fun deleteFile(fileId: UUID) {
        restClient.delete()
            .uri("/api/v1/internal/files/{fileId}", fileId)
            .retrieve()
            .toBodilessEntity()
    }
}