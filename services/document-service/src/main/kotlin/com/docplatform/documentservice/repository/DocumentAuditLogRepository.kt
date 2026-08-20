package com.docplatform.documentservice.repository

import com.docplatform.documentservice.entity.AuditAction
import com.docplatform.documentservice.entity.DocumentAuditLog
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface DocumentAuditLogRepository : JpaRepository<DocumentAuditLog, UUID> {

    fun findByDocumentId(documentId: UUID, pageable: Pageable): Page<DocumentAuditLog>

    fun findByDocumentIdAndAction(documentId: UUID, action: AuditAction, pageable: Pageable): Page<DocumentAuditLog>

    fun findByUserId(userId: UUID, pageable: Pageable): Page<DocumentAuditLog>
}
