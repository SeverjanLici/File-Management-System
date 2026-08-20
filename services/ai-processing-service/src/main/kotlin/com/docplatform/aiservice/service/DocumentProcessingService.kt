package com.docplatform.aiservice.service

import com.docplatform.aiservice.dto.EditProcessingRequest
import com.docplatform.aiservice.dto.ProcessingResultDto
import com.docplatform.aiservice.dto.ProcessingStatusDto
import com.docplatform.aiservice.entity.DocumentProcessing
import com.docplatform.aiservice.entity.ProcessingStatus
import com.docplatform.aiservice.repository.DocumentProcessingRepository
import com.docplatform.common.event.FileUploadedEvent
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID
import kotlin.math.roundToInt

@Service
class DocumentProcessingService(
    private val repository: DocumentProcessingRepository,
    private val aiModelService: AiModelService,
    private val textExtractionService: MinioTextExtractionService,
    private val documentMetadataSyncService: DocumentMetadataSyncService,
    private val eventPublisher: ApplicationEventPublisher
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Async
    fun startProcessing(event: FileUploadedEvent) {

        val processing = repository.findByFileId(event.fileId)
            ?.also {
                if (it.status == ProcessingStatus.COMPLETED) {
                    logger.info("Skipping processing for file ${event.fileId}; already completed")
                    return
                }
            }
            ?: DocumentProcessing(
                fileId = event.fileId,
                fileName = event.fileName,
                storagePath = event.storagePath,
                contentType = event.contentType,
                fileSize = event.fileSize
            )

        //
        processing.fileName = event.fileName
        processing.storagePath = event.storagePath
        processing.contentType = event.contentType
        processing.fileSize = event.fileSize
        //
        processing.status = ProcessingStatus.PROCESSING
        processing.error = null
        processing.startedAt = Instant.now()
        repository.save(processing)

        try {
            logger.info("Processing file: ${event.fileId}")

            val fileType = classifyFileType(processing.contentType, processing.fileName)
            val extension = fileExtension(processing.fileName)
            val extractedText = extractTextFromFile(
                FileUploadedEvent(
                    fileId = processing.fileId,
                    fileName = processing.fileName,
                    storagePath = processing.storagePath,
                    contentType = processing.contentType,
                    fileSize = processing.fileSize ?: 0L,
                    uploadedBy = event.uploadedBy
                )
            )

            val summary = if (extractedText.isBlank()) {
                buildFallbackSummary(processing.fileName, fileType, extension, processing.fileSize)
            } else {
                aiModelService.generateSummary(extractedText)
            }

            val category = if (extractedText.isBlank()) {
                fileType
            } else {
                aiModelService.classifyDocument(extractedText)
            }

            val tags = if (extractedText.isBlank()) {
                setOf(fileType.lowercase())
            } else {
                aiModelService.extractTags(extractedText)
            }

            val confidenceScore = if (extractedText.isBlank()) 0.5 else 0.8

            val metadata = buildMetadata(
                event.copy( // puoi anche usare processing qui se vuoi
                    fileName = processing.fileName,
                    storagePath = processing.storagePath,
                    contentType = processing.contentType,
                    fileSize = processing.fileSize ?: 0L
                ),
                fileType,
                extension,
                summary,
                category,
                tags,
                extractedText
            )

            val completed = repository.findByFileId(event.fileId) ?: processing

            completed.status = ProcessingStatus.COMPLETED
            completed.extractedSummary = summary
            completed.classifiedCategory = category
            completed.generatedTags = tags.toMutableSet()
            completed.generatedMetadata = metadata
            completed.confidenceScore = confidenceScore
            completed.completedAt = Instant.now()
            completed.executionTimeMs =
                if (completed.startedAt != null)
                    completed.completedAt!!.toEpochMilli() - completed.startedAt!!.toEpochMilli()
                else null
            completed.error = null

            repository.save(completed)

            documentMetadataSyncService.syncByFileId(
                fileId = event.fileId,
                summary = summary,
                category = category,
                tags = tags
            )

            logger.info("File {} processing completed", event.fileId)

        } catch (e: Exception) {
            logger.error("Error processing file ${event.fileId}", e)

            val failed = repository.findByFileId(event.fileId) ?: processing
            failed.status = ProcessingStatus.FAILED
            failed.error = e.message ?: "Unknown processing error"
            failed.retryCount = failed.retryCount + 1
            failed.completedAt = Instant.now()
            failed.executionTimeMs =
                if (failed.startedAt != null)
                    failed.completedAt!!.toEpochMilli() - failed.startedAt!!.toEpochMilli()
                else null
            repository.save(failed)
        }
    }

    @Transactional
    fun deleteByFileId(fileId: UUID) {

        repository.deleteProcessingByFileId(fileId)

    }


    fun getProcessingStatus(fileId: UUID): ProcessingStatusDto? {
        return repository.findByFileId(fileId)?.toStatusDto()
    }


    fun getAllProcessing(): List<DocumentProcessing> {
        return repository.findAll()

    }

    fun getProcessingResult(fileId: UUID): ProcessingResultDto? {
        val processing = repository.findByFileId(fileId) ?: return null

        return ProcessingResultDto(
            fileId = processing.fileId,
            status = processing.status,
            summary = processing.extractedSummary,
            category = processing.classifiedCategory,
            tags = processing.generatedTags.toList(),
            metadata = processing.generatedMetadata,
            error = processing.error
        )
    }

    fun retryProcessing(processingId: UUID) {
        val processing = repository.findById(processingId).orElseThrow()

        if (processing.status != ProcessingStatus.FAILED) {
            return
        }
/*
        if (processing.retryCount >= 3) {
            throw IllegalStateException("Retry limit reached")
        }
*/
        processing.status = ProcessingStatus.PROCESSING
        processing.error = null
        processing.startedAt = null
        processing.completedAt = null
        processing.retryCount += 1

        repository.save(processing)
/*
        eventPublisher.publishEvent(
            FileUploadedEvent(
                fileId = processing.fileId,
                fileName = processing.fileName,
                storagePath = processing.storagePath,
                contentType = processing.contentType,
                fileSize = processing.fileSize,
                uploadedBy = UUID.fromString("00000000-0000-0000-0000-000000000000")
            )
        )
*/

         startProcessing(
             FileUploadedEvent(
                 fileId = processing.fileId,
                 fileName = processing.fileName,
                 storagePath = processing.storagePath,
                 contentType = processing.contentType,
                 fileSize = processing.fileSize ?: 0L,
                 uploadedBy = UUID.fromString("00000000-0000-0000-0000-000000000000")
             )
         )

    }

    fun validateProcessing(processingId: UUID,   user:String) {
        val processing = repository.findById(processingId).orElseThrow()
        processing.validated = true
        processing.validatedBy = user
        processing.status=ProcessingStatus.COMPLETED
        repository.save(processing)
    }

    fun overrideMetadata(processingId: UUID, metadata: String) {
        val processing = repository.findById(processingId).orElseThrow()
        if (processing.status != ProcessingStatus.FAILED) {
            throw IllegalStateException("Cannot validate a non-completed job")
        }

        processing.generatedMetadata = metadata
        processing.updatedAt = Instant.now()

        repository.save(processing)
    }

    fun getMetrics(): Map<String, Any> {

        val all = repository.findAll()

        val total = all.size
        val completed = repository.findByStatus(ProcessingStatus.COMPLETED).size
        val failed = all.count { it.status == ProcessingStatus.FAILED }
        val times = all.mapNotNull { it.executionTimeMs }
        val avgTime = if (times.isNotEmpty()) times.average() else 0.0

        return mapOf(
            "total" to total,
            "completed" to completed,
            "failed" to failed,
            "successRate" to if (total > 0) completed.toDouble() / total else 0.0,
            "avgExecutionTimeMs" to avgTime
        )
    }

    fun editProcessing(
        id: UUID,
        request: EditProcessingRequest,
        user: String
    ) {
        val p = repository.findById(id)
            .orElseThrow { RuntimeException("Processing not found") }

        if (p.status != ProcessingStatus.COMPLETED) {
            throw IllegalStateException("Cannot edit before processing is completed")
        }

        request.extractedSummary?.let {
            p.extractedSummary = it
        }

        request.classifiedCategory?.let {
            p.classifiedCategory = it
        }

        request.generatedTags?.let {
            p.generatedTags.clear()
            p.generatedTags.addAll(it)
        }

        p.validated = true
        p.validatedBy = user

        repository.save(p)
    }

    private fun extractTextFromFile(event: FileUploadedEvent): String {
        return runCatching { textExtractionService.extractText(event) }
            .onFailure { ex ->
                logger.warn("Text extraction unavailable for file {}", event.fileId, ex)
            }
            .getOrDefault("")
    }

    private fun fileExtension(fileName: String): String {
        return fileName.substringAfterLast('.', "").lowercase()
    }

    private fun classifyFileType(contentType: String, fileName: String): String {
        val ct = contentType.lowercase()
        val ext = fileExtension(fileName)

        return when {
            ct.startsWith("text/") || ext in setOf("txt", "md", "csv", "json", "xml", "log") -> "Text Document"
            ct.startsWith("image/") || ext in setOf("png", "jpg", "jpeg", "gif", "bmp", "webp", "svg") -> "Image"
            ct.startsWith("video/") || ext in setOf("mp4", "mov", "avi", "mkv", "webm") -> "Video"
            ct.startsWith("audio/") || ext in setOf("mp3", "wav", "aac", "flac", "ogg") -> "Audio"
            ct == "application/pdf" || ext == "pdf" -> "PDF"
            ct.contains("word") || ext in setOf("doc", "docx") -> "Word Document"
            ct.contains("sheet") || ct.contains("excel") || ext in setOf("xls", "xlsx") -> "Spreadsheet"
            ct.contains("presentation") || ct.contains("powerpoint") || ext in setOf("ppt", "pptx") -> "Presentation"
            ct.contains("zip") || ct.contains("archive") || ext in setOf("zip", "rar", "7z", "tar", "gz") -> "Archive"
            else -> "Binary File"
        }
    }

    private fun buildFallbackSummary(
        fileName: String,
        fileType: String,
        extension: String,
        sizeBytes: Long?
    ): String {
        val sizePart = if (sizeBytes != null) {
            val sizeKb = (sizeBytes / 1024.0).roundToInt()
            ", ${sizeKb} KB"
        } else {
            ""
        }
        val extText = if (extension.isBlank()) "unknown extension" else ".${extension}"
        return "This is a $fileType file named '$fileName' ($extText$sizePart) uploaded for processing."
    }

    private fun buildMetadata(
        event: FileUploadedEvent,
        fileType: String,
        extension: String,
        summary: String,
        category: String,
        tags: Set<String>,
        extractedText: String
    ): String {
        val wordCount = extractedText.split(Regex("\\s+")).filter { it.isNotBlank() }.size
        val tagsJson = tags.joinToString(", ") { "\"$it\"" }

        return """
            {
              "fileId": "${event.fileId}",
              "fileName": "${event.fileName}",
              "contentType": "${event.contentType}",
              "fileType": "$fileType",
              "extension": "$extension",
              "sizeBytes": ${event.fileSize},
              "summary": "${escapeJson(summary)}",
              "category": "${escapeJson(category)}",
              "tags": [$tagsJson],
              "wordCount": $wordCount
            }
        """.trimIndent()
    }

    private fun escapeJson(value: String): String {
        return value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun DocumentProcessing.toStatusDto(): ProcessingStatusDto {
        return ProcessingStatusDto(
            id = this.id!!,
            fileId = this.fileId,
            status = this.status,
            generatedMetadata = this.generatedMetadata,
            extractedSummary = this.extractedSummary,
            classifiedCategory = this.classifiedCategory,
            generatedTags = this.generatedTags.toList(),
            error = this.error,
            retryCount = this.retryCount,
            createdAt = this.createdAt,
            updatedAt = this.updatedAt,
            startedAt = this.startedAt,
            completedAt = this.completedAt
        )
    }

}
