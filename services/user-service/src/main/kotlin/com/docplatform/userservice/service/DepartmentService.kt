package com.docplatform.userservice.service

import com.docplatform.common.dto.*
import com.docplatform.common.exception.AccessDeniedException
import com.docplatform.common.exception.ConflictException
import com.docplatform.common.exception.ResourceNotFoundException
import com.docplatform.userservice.entity.Department
import com.docplatform.userservice.repository.DepartmentRepository
import com.docplatform.userservice.repository.UserRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class DepartmentService(
    private val departmentRepository: DepartmentRepository,
    private val userRepository: UserRepository,
    private val documentShareCleanupService: DocumentShareCleanupService
) {

    @Transactional
    fun createDepartment(request: CreateDepartmentRequest, userRole: Role): DepartmentDto {
        if (userRole != Role.ADMIN) {
            throw AccessDeniedException("Only admins can create departments")
        }

        if (departmentRepository.existsByName(request.name)) {
            throw ConflictException("Department with name '${request.name}' already exists")
        }

        val department = Department(
            name = request.name,
            description = request.description
        )

        return departmentRepository.save(department).toDto()
    }

    @Transactional(readOnly = true)
    fun getDepartment(id: UUID): DepartmentDto {
        val department = departmentRepository.findByIdWithMembers(id)
            .orElseThrow { ResourceNotFoundException("Department", id) }
        return department.toDto()
    }

    @Transactional(readOnly = true)
    fun listDepartments(search: String?, pageable: Pageable): Page<DepartmentDto> {
        val page = if (search.isNullOrBlank()) {
            departmentRepository.findAll(pageable)
        } else {
            departmentRepository.searchDepartments(search, pageable)
        }
        return page.map { it.toDto() }
    }

    @Transactional
    fun updateDepartment(id: UUID, request: UpdateDepartmentRequest, userRole: Role): DepartmentDto {
        if (userRole != Role.ADMIN) {
            throw AccessDeniedException("Only admins can update departments")
        }

        val department = departmentRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("Department", id) }

        request.name?.let { newName ->
            if (newName != department.name && departmentRepository.existsByName(newName)) {
                throw ConflictException("Department with name '$newName' already exists")
            }
            department.name = newName
        }
        request.description?.let { department.description = it }

        return departmentRepository.save(department).toDto()
    }

    @Transactional
    fun deleteDepartment(id: UUID, userRole: Role) {
        if (userRole != Role.ADMIN) {
            throw AccessDeniedException("Only admins can delete departments")
        }

        val department = departmentRepository.findByIdWithMembers(id)
            .orElseThrow { ResourceNotFoundException("Department", id) }

        documentShareCleanupService.removeDepartmentShares(id)

        // Remove all members from the department
        department.members.forEach { user ->
            user.departments.remove(department)
        }
        department.members.clear()

        departmentRepository.delete(department)
    }

    @Transactional
    fun addMember(
        departmentId: UUID,
        userId: UUID,
        userRole: Role,
        currentUserExternalId: String
    ): DepartmentDto {
        if (userRole != Role.ADMIN && userRole != Role.MANAGER) {
            throw AccessDeniedException("Only admins and managers can add department members")
        }

        val department = departmentRepository.findByIdWithMembers(departmentId)
            .orElseThrow { ResourceNotFoundException("Department", departmentId) }

        assertManagerCanManageDepartment(userRole, currentUserExternalId, department)

        val user = userRepository.findById(userId)
            .orElseThrow { ResourceNotFoundException("User", userId) }

        if (department.members.any { it.id == userId }) {
            throw ConflictException("User is already a member of this department")
        }

        department.members.add(user)
        user.departments.add(department)

        return departmentRepository.save(department).toDto()
    }

    @Transactional
    fun removeMember(
        departmentId: UUID,
        userId: UUID,
        userRole: Role,
        currentUserExternalId: String
    ): DepartmentDto {
        if (userRole != Role.ADMIN && userRole != Role.MANAGER) {
            throw AccessDeniedException("Only admins and managers can remove department members")
        }

        val department = departmentRepository.findByIdWithMembers(departmentId)
            .orElseThrow { ResourceNotFoundException("Department", departmentId) }

        assertManagerCanManageDepartment(userRole, currentUserExternalId, department)

        val user = userRepository.findById(userId)
            .orElseThrow { ResourceNotFoundException("User", userId) }

        if (!department.members.any { it.id == userId }) {
            throw ResourceNotFoundException("User not found in department", userId)
        }

        department.members.removeIf { it.id == userId }
        user.departments.removeIf { it.id == departmentId }

        return departmentRepository.save(department).toDto()
    }

    @Transactional(readOnly = true)
    fun getDepartmentsByUserId(userId: UUID): List<DepartmentDto> {
        return departmentRepository.findByUserId(userId).map { it.toDto() }
    }

    private fun assertManagerCanManageDepartment(
        userRole: Role,
        currentUserExternalId: String,
        department: Department
    ) {
        if (userRole != Role.MANAGER) {
            return
        }

        val currentUser = userRepository.findByExternalIdWithDepartments(currentUserExternalId)
            .orElseThrow { ResourceNotFoundException("User", currentUserExternalId) }

        if (currentUser.departments.none { it.id == department.id }) {
            throw AccessDeniedException("Managers can only manage departments they belong to")
        }
    }

    private fun Department.toDto(): DepartmentDto {
        return DepartmentDto(
            id = this.id!!,
            name = this.name,
            description = this.description,
            memberCount = this.members.size,
            createdAt = this.createdAt,
            updatedAt = this.updatedAt
        )
    }
}
