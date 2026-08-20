package com.docplatform.fileservice.repository

import com.docplatform.fileservice.entity.StoredFile
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface StoredFileRepository : JpaRepository<StoredFile, UUID> {

    fun findByUploadedBy(uploadedBy: UUID, pageable: Pageable): Page<StoredFile>

    fun existsByObjectName(objectName: String): Boolean
}
