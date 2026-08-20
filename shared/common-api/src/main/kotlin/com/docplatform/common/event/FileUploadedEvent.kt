package com.docplatform.common.event

import java.util.UUID

data class FileUploadedEvent(
    val fileId: UUID,
    val fileName: String,
    val storagePath: String,
    val contentType: String,
    val fileSize: Long,
    val uploadedBy: UUID
)

