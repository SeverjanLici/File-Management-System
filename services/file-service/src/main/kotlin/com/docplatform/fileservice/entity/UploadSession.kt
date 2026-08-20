package com.docplatform.fileservice.entity

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "upload_sessions")
class UploadSession(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @Column(nullable = false)
    var uploadId: String,

    @Column(nullable = false)
    var fileName: String,

    @Column(nullable = false)
    var fileSize: Long,

    @Column(nullable = false)
    var mimeType: String,

    @Column(nullable = false)
    var uploadedBy: UUID,

    @Column(name = "\"offset\"", nullable = false)
    var offset: Long = 0,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: UploadStatus = UploadStatus.IN_PROGRESS,

    @Column
    var storagePath: String? = null,

    @Column(nullable = false)
    var expiresAt: Instant = Instant.now().plusSeconds(86400), // 24 hours

    @Column(nullable = false, updatable = false)
    var createdAt: Instant = Instant.now(),

    @Column(nullable = false)
    var updatedAt: Instant = Instant.now()
) {
    @PreUpdate
    fun onUpdate() {
        updatedAt = Instant.now()
    }
}

enum class UploadStatus {
    IN_PROGRESS,
    COMPLETED,
    FAILED,
    EXPIRED
}
