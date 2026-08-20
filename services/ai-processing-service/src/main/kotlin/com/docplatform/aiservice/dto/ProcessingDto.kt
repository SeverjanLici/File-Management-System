package com.docplatform.aiservice.dto

import com.docplatform.aiservice.entity.ProcessingStatus
import java.time.Instant
import java.util.UUID

data class ProcessingStatusDto(
    val id: UUID,
    val fileId: UUID,
    val status: ProcessingStatus,
    val generatedMetadata: String?,
    val extractedSummary: String?,
    val classifiedCategory: String?,
    val generatedTags: List<String>,
    val error: String?,
    val retryCount: Int,
    val createdAt: Instant,
    val updatedAt: Instant,
    val startedAt: Instant?,
    val completedAt: Instant?
)

data class ProcessingResultDto(
    val fileId: UUID,
    val status: ProcessingStatus,
    val summary: String?,
    val category: String?,
    val tags: List<String>,
    val metadata: String?,
    val error: String?
)
