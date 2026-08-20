package com.docplatform.common.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

data class DocumentDto(
    val id: UUID,
    val name: String,
    val description: String?,
    val category: String?,
    val tags: List<String> = emptyList(),
    val summary: String?,
    val currentVersionNumber: Int,
    val fileId: UUID,
    val fileName: String,
    val fileSize: Long,
    val mimeType: String,
    val ownerId: UUID,
    val ownerName: String,
    val permissions: List<Permission> = emptyList(),
    val shares: List<ShareDto> = emptyList(),
    val versions: List<DocumentVersionDto> = emptyList(),
    val createdAt: Instant,
    val updatedAt: Instant
)

data class DocumentSummaryDto(
    val id: UUID,
    val name: String,
    val category: String?,
    val tags: List<String> = emptyList(),
    val summary: String? = null,
    val currentVersionNumber: Int,
    val fileId: UUID,
    val fileName: String,
    val fileSize: Long,
    val mimeType: String,
    val ownerId: UUID,
    val ownerName: String,
    val createdAt: Instant
)

data class CreateDocumentRequest(
    @field:NotBlank(message = "Document name is required")
    @field:Size(min = 1, max = 255, message = "Document name must be between 1 and 255 characters")
    val name: String,

    @field:Size(max = 1000, message = "Description must be less than 1000 characters")
    val description: String? = null,

    @field:Size(max = 120, message = "Category must be less than 120 characters")
    val category: String? = null,

    val tags: List<@Size(min = 1, max = 80, message = "Each tag must be between 1 and 80 characters") String> = emptyList(),

    @field:Size(max = 2000, message = "Summary must be less than 2000 characters")
    val summary: String? = null,

    val fileId: UUID,
    val fileName: String,
    val fileSize: Long,
    val mimeType: String
)

data class CreateDocumentVersionRequest(
    val fileId: UUID,
    val fileName: String,
    val fileSize: Long,
    val mimeType: String
)

data class UpdateDocumentRequest(
    @field:Size(min = 1, max = 255, message = "Document name must be between 1 and 255 characters")
    val name: String? = null,

    @field:Size(max = 1000, message = "Description must be less than 1000 characters")
    val description: String? = null,

    @field:Size(max = 120, message = "Category must be less than 120 characters")
    val category: String? = null,

    val tags: List<@Size(min = 1, max = 80, message = "Each tag must be between 1 and 80 characters") String>? = null,

    @field:Size(max = 2000, message = "Summary must be less than 2000 characters")
    val summary: String? = null
)

data class SyncDocumentAiMetadataRequest(
    @field:Size(max = 2000, message = "Summary must be less than 2000 characters")
    val summary: String? = null,

    @field:Size(max = 120, message = "Category must be less than 120 characters")
    val category: String? = null,

    val tags: List<@Size(min = 1, max = 80, message = "Each tag must be between 1 and 80 characters") String> = emptyList()
)

data class DocumentVersionDto(
    val id: UUID,
    val versionNumber: Int,
    val fileId: UUID,
    val fileName: String,
    val fileSize: Long,
    val mimeType: String,
    val createdBy: UUID,
    val createdByName: String,
    val createdAt: Instant
)

enum class Permission {
    VIEW,
    EDIT,
    DELETE,
    SHARE,
    ADMIN
}
