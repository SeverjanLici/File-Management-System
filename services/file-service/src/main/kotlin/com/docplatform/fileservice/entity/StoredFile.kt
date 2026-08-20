package com.docplatform.fileservice.entity

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "stored_files")
class StoredFile(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @Column(nullable = false)
    var originalName: String,

    @Column(nullable = false)
    var storagePath: String,

    @Column(nullable = false)
    var bucketName: String,

    @Column(nullable = false)
    var objectName: String,

    @Column(nullable = false)
    var size: Long,

    @Column(nullable = false)
    var mimeType: String,

    @Column(nullable = false)
    var uploadedBy: UUID,

    @Column
    var checksum: String? = null,

    @Column(nullable = false, updatable = false)
    var createdAt: Instant = Instant.now()
)
