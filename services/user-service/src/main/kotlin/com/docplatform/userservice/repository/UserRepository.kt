package com.docplatform.userservice.repository

import com.docplatform.userservice.entity.User
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface UserRepository : JpaRepository<User, UUID> {

    fun findByExternalId(externalId: String): Optional<User>

    fun findByEmail(email: String): Optional<User>

    fun existsByExternalId(externalId: String): Boolean

    fun existsByEmail(email: String): Boolean

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.departments WHERE u.id = :id")
    fun findByIdWithDepartments(id: UUID): Optional<User>

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.departments WHERE u.externalId = :externalId")
    fun findByExternalIdWithDepartments(externalId: String): Optional<User>

    @Query("SELECT DISTINCT u FROM User u LEFT JOIN u.departments d WHERE d.id = :departmentId")
    fun findByDepartmentId(departmentId: UUID, pageable: Pageable): Page<User>

    @Query("SELECT u FROM User u WHERE LOWER(u.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%'))")
    fun searchUsers(search: String, pageable: Pageable): Page<User>
}
