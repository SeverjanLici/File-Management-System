package com.docplatform.userservice.entity

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "departments")
class Department(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @Column(nullable = false, unique = true)
    var name: String,

    @Column
    var description: String? = null,

    @ManyToMany(mappedBy = "departments", fetch = FetchType.LAZY)
    var members: MutableSet<User> = mutableSetOf(),

    @Column(nullable = false, updatable = false)
    var createdAt: Instant = Instant.now(),

    @Column(nullable = false)
    var updatedAt: Instant = Instant.now()
) {
    @PreUpdate
    fun onUpdate() {
        updatedAt = Instant.now()
    }

    val memberCount: Int
        get() = members.size
}
