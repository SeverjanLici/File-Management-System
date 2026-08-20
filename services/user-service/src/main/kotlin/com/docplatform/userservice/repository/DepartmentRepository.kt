package com.docplatform.userservice.repository

import com.docplatform.userservice.entity.Department
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface DepartmentRepository : JpaRepository<Department, UUID> {

    fun findByName(name: String): Optional<Department>

    fun existsByName(name: String): Boolean

    @Query("SELECT d FROM Department d LEFT JOIN FETCH d.members WHERE d.id = :id")
    fun findByIdWithMembers(id: UUID): Optional<Department>

    @Query("SELECT d FROM Department d WHERE LOWER(d.name) LIKE LOWER(CONCAT('%', :search, '%'))")
    fun searchDepartments(search: String, pageable: Pageable): Page<Department>

    @Query("SELECT d FROM Department d JOIN d.members m WHERE m.id = :userId")
    fun findByUserId(userId: UUID): List<Department>
}
