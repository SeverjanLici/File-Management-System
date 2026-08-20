package com.docplatform.common.dto

import java.time.Instant
import java.util.UUID

data class FileDto(
    val id: UUID,
    val originalName: String,
    val storagePath: String,
    val size: Long,
    val mimeType: String,
    val uploadedBy: UUID,
    val uploadCompleted: Boolean,
    val createdAt: Instant
)

data class UploadSessionDto(
    val id: UUID,
    val uploadUrl: String,
    val fileId: UUID,
    val offset: Long,
    val length: Long,
    val expiresAt: Instant
)

data class UploadCompleteDto(
    val fileId: UUID,
    val fileName: String,
    val size: Long,
    val mimeType: String
)

data class PresignedUrlDto(
    val url: String,
    val expiresAt: Instant
)
