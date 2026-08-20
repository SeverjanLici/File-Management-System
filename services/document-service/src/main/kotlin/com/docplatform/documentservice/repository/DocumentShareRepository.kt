package com.docplatform.documentservice.repository

import com.docplatform.common.dto.ShareType
import com.docplatform.documentservice.entity.DocumentShare
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface DocumentShareRepository : JpaRepository<DocumentShare, UUID> {

    fun findByDocumentId(documentId: UUID): List<DocumentShare>

    fun findByDocumentIdAndShareTypeAndTargetId(documentId: UUID, shareType: ShareType, targetId: UUID): DocumentShare?

    fun findByDocumentIdAndShareTypeAndTargetName(documentId: UUID, shareType: ShareType, targetName: String): DocumentShare?

    fun findByDocumentIdAndShareType(documentId: UUID, shareType: ShareType): List<DocumentShare>

    fun deleteByDocumentIdAndId(documentId: UUID, shareId: UUID)

    fun deleteByShareTypeAndTargetId(shareType: ShareType, targetId: UUID): Long
}
