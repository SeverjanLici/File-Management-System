package com.docplatform.documentservice.entity

import com.docplatform.common.dto.Permission
import com.docplatform.common.dto.ShareType
import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "document_shares")
class DocumentShare(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    var document: Document? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var shareType: ShareType,

    @Column
    var targetId: UUID? = null,

    @Column
    var targetName: String? = null,

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "document_share_permissions", joinColumns = [JoinColumn(name = "share_id")])
    @Enumerated(EnumType.STRING)
    @Column(name = "permission")
    var permissions: MutableSet<Permission> = mutableSetOf(),

    @Column(nullable = false)
    var createdBy: UUID,

    @Column(nullable = false, updatable = false)
    var createdAt: Instant = Instant.now()
)
