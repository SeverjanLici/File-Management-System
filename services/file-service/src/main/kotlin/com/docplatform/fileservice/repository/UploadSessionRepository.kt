package com.docplatform.fileservice.repository

import com.docplatform.fileservice.entity.UploadSession
import com.docplatform.fileservice.entity.UploadStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.Optional
import java.util.UUID

@Repository
interface UploadSessionRepository : JpaRepository<UploadSession, UUID> {

    fun findByUploadId(uploadId: String): Optional<UploadSession>

    fun findByUploadedByAndStatus(uploadedBy: UUID, status: UploadStatus): List<UploadSession>

    @Modifying
    @Query("UPDATE UploadSession u SET u.status = 'EXPIRED' WHERE u.expiresAt < :now AND u.status = 'IN_PROGRESS'")
    fun expireOldSessions(now: Instant): Int

    fun deleteByStatusAndExpiresAtBefore(status: UploadStatus, before: Instant): Long
}
