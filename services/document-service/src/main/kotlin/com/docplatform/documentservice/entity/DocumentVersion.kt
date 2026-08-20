package com.docplatform.documentservice.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "document_versions",
    uniqueConstraints = [UniqueConstraint(columnNames = ["document_id", "version_number"])]
)
class DocumentVersion(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    var document: Document? = null,

    @Column(name = "version_number", nullable = false)
    var versionNumber: Int,

    @Column(nullable = false)
    var fileId: UUID,

    @Column(nullable = false)
    var fileName: String,

    @Column(nullable = false)
    var fileSize: Long,

    @Column(nullable = false)
    var mimeType: String,

    @Column(nullable = false)
    var createdBy: UUID,

    @Column(nullable = false)
    var createdByName: String,

    @Column(nullable = false, updatable = false)
    var createdAt: Instant = Instant.now()
)