package com.docplatform.documentservice.entity

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "document_audit_logs")
class DocumentAuditLog(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    var document: Document? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var action: AuditAction,

    @Column(nullable = false)
    var userId: UUID,

    @Column(nullable = false)
    var userName: String,

    @Column(length = 1000)
    var details: String? = null,

    @Column(nullable = false, updatable = false)
    var createdAt: Instant = Instant.now()
)

enum class AuditAction {
    CREATED,
    VERSION_ADDED,
    VIEWED,
    UPDATED,
    DELETED,
    DOWNLOADED,
    SHARED,
    UNSHARED,
    PERMISSION_CHANGED
}
