package com.docplatform.documentservice.messaging

import com.docplatform.common.event.DocumentDeletedEvent
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service

@Service
class DocumentEventProducer(
    private val kafkaTemplate: KafkaTemplate<String, Any>,

    @Value("\${app.kafka.topics.documentDeleted}")
    private val documentDeletedTopic: String
) {

    fun publishDocumentDeleted(event: DocumentDeletedEvent) {
        kafkaTemplate.send(
            documentDeletedTopic,
            event.documentId.toString(),
            event
        )
    }
}