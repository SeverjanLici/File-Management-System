package com.docplatform.fileservice.config

import com.docplatform.common.event.FileUploadedEvent
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import org.slf4j.LoggerFactory

@Service
class KafkaPublisher(
    private val kafkaTemplate: KafkaTemplate<String, FileUploadedEvent>,
    @Value("\${app.kafka.topics.fileUploaded:file-uploaded}")
    private val fileUploadedTopic: String
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun publishFileUploadedEvent(event: FileUploadedEvent) {
        logger.info("Publishing file uploaded event for file: ${event.fileId}")
        try {
            kafkaTemplate.send(fileUploadedTopic, event.fileId.toString(), event)
            logger.info("Successfully published event for file: ${event.fileId}")
        } catch (e: Exception) {
            logger.error("Failed to publish event for file: ${event.fileId}", e)
        }
    }
}
