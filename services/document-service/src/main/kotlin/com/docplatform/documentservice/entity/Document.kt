package com.docplatform.documentservice.entity

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "documents")
class Document(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @Column(nullable = false)
    var name: String,

    @Column(length = 1000)
    var description: String? = null,

    @Column(length = 120)
    var category: String? = null,

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "document_tags", joinColumns = [JoinColumn(name = "document_id")])
    @Column(name = "tag", nullable = false, length = 80)
    var tags: MutableSet<String> = mutableSetOf(),

    @Column(length = 2000)
    var summary: String? = null,

    @Column(nullable = false)
    var currentVersionNumber: Int = 1,

    @Column(nullable = false)
    var fileId: UUID,

    @Column(nullable = false)
    var fileName: String,

    @Column(nullable = false)
    var fileSize: Long,

    @Column(nullable = false)
    var mimeType: String,

    @Column(nullable = false)
    var ownerId: UUID,

    @Column(nullable = false)
    var ownerName: String,

    @Column(nullable = false)
    var companyWide: Boolean = false,

    @OneToMany(mappedBy = "document", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    var shares: MutableSet<DocumentShare> = mutableSetOf(),

    @OneToMany(mappedBy = "document", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("versionNumber DESC")
    var versions: MutableSet<DocumentVersion> = linkedSetOf(),

    @OneToMany(mappedBy = "document", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    var auditLogs: MutableList<DocumentAuditLog> = mutableListOf(),

    @Column(nullable = false, updatable = false)
    var createdAt: Instant = Instant.now(),

    @Column(nullable = false)
    var updatedAt: Instant = Instant.now()
) {
    @PreUpdate
    fun onUpdate() {
        updatedAt = Instant.now()
    }

    fun addShare(share: DocumentShare) {
        shares.add(share)
        share.document = this
    }

    fun removeShare(share: DocumentShare) {
        shares.remove(share)
        share.document = null
    }

    fun addVersion(version: DocumentVersion) {
        versions.add(version)
        version.document = this
        currentVersionNumber = version.versionNumber
        fileId = version.fileId
        fileName = version.fileName
        fileSize = version.fileSize
        mimeType = version.mimeType
    }
}
