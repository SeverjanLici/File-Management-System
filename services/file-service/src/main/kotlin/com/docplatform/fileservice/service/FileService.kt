package com.docplatform.fileservice.service

import com.docplatform.common.dto.FileDto
import com.docplatform.common.dto.PresignedUrlDto
import com.docplatform.common.exception.ResourceNotFoundException
import com.docplatform.fileservice.entity.StoredFile
import com.docplatform.fileservice.repository.StoredFileRepository
import org.springframework.core.io.InputStreamResource
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class FileService(
    private val storedFileRepository: StoredFileRepository,
    private val minioStorageService: MinioStorageService
) {

    @Transactional(readOnly = true)
    fun getFile(id: UUID): FileDto {
        val file = storedFileRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("File", id) }
        return file.toDto()
    }

    @Transactional(readOnly = true)
    fun listUserFiles(userId: UUID, pageable: Pageable): Page<FileDto> {
        return storedFileRepository.findByUploadedBy(userId, pageable).map { it.toDto() }
    }

    fun downloadFile(id: UUID): ResponseEntity<InputStreamResource> {
        val file = storedFileRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("File", id) }

        val inputStream = minioStorageService.downloadFile(file.objectName)

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"${file.originalName}\"")
            .contentType(MediaType.parseMediaType(file.mimeType))
            .contentLength(file.size)
            .body(InputStreamResource(inputStream))
    }

    fun getPresignedUrl(id: UUID, expirationMinutes: Int = 60): PresignedUrlDto {
        val file = storedFileRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("File", id) }

        val (url, expiresAt) = minioStorageService.getPresignedUrl(file.objectName, expirationMinutes, file.originalName)

        return PresignedUrlDto(url = url, expiresAt = expiresAt)
    }

    fun getPreviewUrl(id: UUID, expirationMinutes: Int = 60): PresignedUrlDto {
        val file = storedFileRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("File", id) }

        val (url, expiresAt) = minioStorageService.getPresignedUrl(file.objectName, expirationMinutes)

        return PresignedUrlDto(url = url, expiresAt = expiresAt)
    }

    @Transactional
    fun deleteFile(id: UUID) {
        val file = storedFileRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("File", id) }

        minioStorageService.deleteFile(file.objectName)
        storedFileRepository.delete(file)
    }

    private fun StoredFile.toDto(): FileDto {
        return FileDto(
            id = this.id!!,
            originalName = this.originalName,
            storagePath = this.storagePath,
            size = this.size,
            mimeType = this.mimeType,
            uploadedBy = this.uploadedBy,
            uploadCompleted = true,
            createdAt = this.createdAt
        )
    }
}
