package com.docplatform.common.event

import java.time.Instant
import java.util.UUID

data class DocumentDeletedEvent(
    val documentId: UUID?,
    val fileId: UUID,
    val deletedBy: UUID,
    val deletedAt: Instant = Instant.now()
)