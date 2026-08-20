package com.docplatform.aiservice.config

import com.docplatform.aiservice.service.DocumentProcessingService
import com.docplatform.common.event.DocumentDeletedEvent
import com.docplatform.common.event.FileUploadedEvent
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Service
import org.slf4j.LoggerFactory

@Service
class KafkaConsumer(
    private val documentProcessingService: DocumentProcessingService
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @KafkaListener(
        topics = ["\${app.kafka.topics.fileUploaded:file-uploaded}"],
        groupId = "\${spring.kafka.consumer.group-id}"
    )
    fun consumeFileUploadedEvent(event: FileUploadedEvent) {

        logger.info("Received file uploaded event for file: ${event.fileId}")

        try {
            documentProcessingService.startProcessing(event)
        } catch (e: Exception) {
            logger.error("Error processing file ${event.fileId}", e)
        }
    }

    @KafkaListener(
        topics = ["\${app.kafka.topics.documentDeleted:document-deleted}"],
        groupId = "\${spring.kafka.consumer.group-id}"
    )
    fun consumeDocumentDeletedEvent(event: DocumentDeletedEvent) {

        logger.info("Received document deleted event for file: ${event.fileId}")

        try {
            documentProcessingService.deleteByFileId(event.fileId)
        } catch (e: Exception) {
            logger.error("Error deleting processing for file ${event.fileId}", e)
        }
    }
}