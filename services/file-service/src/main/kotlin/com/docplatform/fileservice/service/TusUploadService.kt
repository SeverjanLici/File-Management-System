package com.docplatform.fileservice.service

import com.docplatform.common.dto.UploadCompleteDto
import com.docplatform.common.event.FileUploadedEvent
import com.docplatform.common.exception.ResourceNotFoundException
import com.docplatform.common.exception.UploadException
import com.docplatform.fileservice.config.KafkaPublisher
import com.docplatform.fileservice.entity.StoredFile
import com.docplatform.fileservice.entity.UploadSession
import com.docplatform.fileservice.entity.UploadStatus
import com.docplatform.fileservice.repository.StoredFileRepository
import com.docplatform.fileservice.repository.UploadSessionRepository
import me.desair.tus.server.TusFileUploadService
import me.desair.tus.server.upload.UploadInfo
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse

@Service
class TusUploadService(
    private val tusFileUploadService: TusFileUploadService,
    private val minioStorageService: MinioStorageService,
    private val uploadSessionRepository: UploadSessionRepository,
    private val storedFileRepository: StoredFileRepository,
    private val kafkaPublisher: KafkaPublisher,

    @Value("\${minio.bucket-name}")
    private val bucketName: String
) {
    private val logger = LoggerFactory.getLogger(TusUploadService::class.java)

    fun processUpload(request: HttpServletRequest, response: HttpServletResponse) {
        tusFileUploadService.process(request, response)
    }

    @Transactional
    fun createUploadSession(
        uploadId: String,
        fileName: String,
        fileSize: Long,
        mimeType: String,
        uploadedBy: UUID
    ): UploadSession {
        val session = UploadSession(
            uploadId = uploadId,
            fileName = fileName,
            fileSize = fileSize,
            mimeType = mimeType,
            uploadedBy = uploadedBy
        )
        return uploadSessionRepository.save(session)
    }

    @Transactional
    fun updateUploadProgress(uploadId: String, offset: Long) {
        val session = uploadSessionRepository.findByUploadId(uploadId)
            .orElseThrow { ResourceNotFoundException("Upload session", uploadId) }

        session.offset = offset
        uploadSessionRepository.save(session)
    }

    @Transactional
    fun completeUpload(uploadId: String): UploadCompleteDto {
        val session = uploadSessionRepository.findByUploadId(uploadId)
            .orElseThrow { ResourceNotFoundException("Upload session", uploadId) }

        try {
            tusFileUploadService.getUploadInfo(uploadId)
                ?: throw UploadException("Upload info not found for: $uploadId")

            val inputStream = tusFileUploadService.getUploadedBytes(uploadId)
                ?: throw UploadException("Upload data not found for: $uploadId")

            val objectName = generateObjectName(session.fileName, session.uploadedBy)

            val storagePath = minioStorageService.uploadFile(
                objectName = objectName,
                inputStream = inputStream,
                size = session.fileSize,
                contentType = session.mimeType
            )

            val storedFile = StoredFile(
                originalName = session.fileName,
                storagePath = storagePath,
                bucketName = bucketName,
                objectName = objectName,
                size = session.fileSize,
                mimeType = session.mimeType,
                uploadedBy = session.uploadedBy
            )
            val savedFile = storedFileRepository.save(storedFile)

            session.status = UploadStatus.COMPLETED
            session.storagePath = storagePath
            uploadSessionRepository.save(session)

            // Non-blocking background processing trigger.
            kafkaPublisher.publishFileUploadedEvent(
                FileUploadedEvent(
                    fileId = savedFile.id!!,
                    fileName = savedFile.originalName,
                    storagePath = storagePath,
                    contentType = savedFile.mimeType,
                    fileSize = savedFile.size,
                    uploadedBy = savedFile.uploadedBy
                )
            )

            tusFileUploadService.deleteUpload(uploadId)

            logger.info("Upload completed: ${savedFile.id} -> $storagePath")

            return UploadCompleteDto(
                fileId = savedFile.id!!,
                fileName = savedFile.originalName,
                size = savedFile.size,
                mimeType = savedFile.mimeType
            )
        } catch (e: Exception) {
            session.status = UploadStatus.FAILED
            uploadSessionRepository.save(session)
            logger.error("Upload failed: $uploadId", e)
            throw UploadException("Failed to complete upload: ${e.message}", e)
        }
    }

    fun getUploadInfo(uploadId: String): UploadInfo? {
        return tusFileUploadService.getUploadInfo(uploadId)
    }

    @Transactional(readOnly = true)
    fun getUploadSession(uploadId: String): UploadSession {
        return uploadSessionRepository.findByUploadId(uploadId)
            .orElseThrow { ResourceNotFoundException("Upload session", uploadId) }
    }

    @Transactional
    fun cancelUpload(uploadId: String) {
        val session = uploadSessionRepository.findByUploadId(uploadId).orElse(null)
        if (session != null) {
            session.status = UploadStatus.EXPIRED
            uploadSessionRepository.save(session)
        }
        try {
            tusFileUploadService.deleteUpload(uploadId)
        } catch (e: Exception) {
            logger.warn("Failed to delete TUS upload: $uploadId", e)
        }
    }

    private fun generateObjectName(fileName: String, uploadedBy: UUID): String {
        val timestamp = Instant.now().toEpochMilli()
        val uuid = UUID.randomUUID().toString().take(8)
        val extension = fileName.substringAfterLast('.', "")
        val baseName = fileName.substringBeforeLast('.').take(50)
        return "files/$uploadedBy/${timestamp}_${uuid}_$baseName.$extension"
    }
}
