package com.docplatform.aiservice.repository

import com.docplatform.aiservice.entity.DocumentProcessing
import com.docplatform.aiservice.entity.ProcessingStatus
import jakarta.transaction.Transactional
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface DocumentProcessingRepository : JpaRepository<DocumentProcessing, UUID> {
    fun findByFileId(fileId: UUID): DocumentProcessing?
    fun findByStatus(status: ProcessingStatus): List<DocumentProcessing>
    fun findByStatusAndRetryCountLessThan(status: ProcessingStatus, retryCount: Int): List<DocumentProcessing>

    @Modifying
    @Transactional
    @Query("DELETE FROM DocumentProcessing dp WHERE dp.fileId = :fileId")
    fun deleteProcessingByFileId(fileId: UUID): Int
}
