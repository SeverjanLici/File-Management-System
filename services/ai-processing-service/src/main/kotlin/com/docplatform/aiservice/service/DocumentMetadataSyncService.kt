package com.docplatform.aiservice.service

import com.docplatform.common.dto.SyncDocumentAiMetadataRequest
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import java.util.UUID

@Service
class DocumentMetadataSyncService(
    webClientBuilder: WebClient.Builder,
    @Value("\${services.document-service.url:http://localhost:8082}")
    documentServiceUrl: String
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val client = webClientBuilder.baseUrl(documentServiceUrl).build()

    fun syncByFileId(fileId: UUID, summary: String?, category: String?, tags: Set<String>) {
        val request = SyncDocumentAiMetadataRequest(
            summary = summary,
            category = category,
            tags = tags.toList()
        )

        try {
            client.post()
                .uri("/api/v1/internal/documents/metadata/file/{fileId}", fileId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .toBodilessEntity()
                .block()
        } catch (ex: Exception) {
            logger.warn("Failed to sync AI metadata to document-service for file {}", fileId, ex)
        }
    }
}

