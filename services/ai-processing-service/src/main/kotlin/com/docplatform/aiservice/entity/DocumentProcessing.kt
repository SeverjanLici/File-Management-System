package com.docplatform.aiservice.entity

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

enum class ProcessingStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED
}

@Entity
@Table(name = "document_processing")
class DocumentProcessing(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,


    @Column(nullable = false, unique = true)
    var fileId: UUID,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: ProcessingStatus = ProcessingStatus.PENDING,

    @Column(columnDefinition = "TEXT")
    var generatedMetadata: String? = null,

    @Column(columnDefinition = "TEXT")
    var extractedSummary: String? = null,

    @Column(columnDefinition = "TEXT")
    var classifiedCategory: String? = null,

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "processing_generated_tags", joinColumns = [JoinColumn(name = "processing_id")])
    @Column(name = "tag")
    var generatedTags: MutableSet<String> = mutableSetOf(),

    @Column(columnDefinition = "TEXT")
    var error: String? = null,

    var retryCount: Int = 0,

    @Column(nullable = false, updatable = false)
    var createdAt: Instant = Instant.now(),

    @Column(nullable = false)
    var updatedAt: Instant = Instant.now(),

    var startedAt: Instant? = null,

    var completedAt: Instant? = null,

    //
    var executionTimeMs: Long? = null,
    var confidenceScore: Double? = null,
    
    // Add default to column definition or make it nullable to avoid DDL error during schema update
    @Column(columnDefinition = "boolean default false")
    var validated: Boolean = false,

    var validatedBy: String? = null,

    var fileName: String,
    var storagePath: String,
    var contentType: String,
    @Column(name="file_size")
    var fileSize: Long? = null,
    //

) {
    @PreUpdate
    fun onUpdate() {
        updatedAt = Instant.now()
    }
}
