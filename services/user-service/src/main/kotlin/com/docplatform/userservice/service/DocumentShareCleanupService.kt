package com.docplatform.userservice.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import java.util.UUID

@Service
class DocumentShareCleanupService(
    @Value("\${services.document-service.url:http://localhost:8082}")
    private val documentServiceUrl: String
) {
    private val restClient = RestClient.builder()
        .baseUrl(documentServiceUrl)
        .build()

    fun removeDepartmentShares(departmentId: UUID) {
        restClient.delete()
            .uri("/api/v1/internal/departments/{departmentId}/shares", departmentId)
            .retrieve()
            .toBodilessEntity()
    }
}