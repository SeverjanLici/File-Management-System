package com.docplatform.userservice.service

import com.docplatform.common.dto.*
import com.docplatform.common.exception.AccessDeniedException
import com.docplatform.common.exception.ResourceNotFoundException
import com.docplatform.security.model.AuthenticatedUser
import com.docplatform.userservice.entity.User
import com.docplatform.userservice.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class UserService(
    private val userRepository: UserRepository
) {
    private val logger = LoggerFactory.getLogger(UserService::class.java)

    private fun trace(title: String, message: String) {
        logger.info("""


#################### USER SERVICE: $title ####################
$message
#############################################################


""".trimIndent())
    }

    @Transactional
    fun syncUser(authenticatedUser: AuthenticatedUser): UserDto {
        trace("SYNC START", "externalId=${authenticatedUser.externalId}\nemail=${authenticatedUser.email}\nname=${authenticatedUser.firstName} ${authenticatedUser.lastName}")
        val existingUser = userRepository.findByExternalIdWithDepartments(authenticatedUser.externalId)
        trace("LOOKUP BY EXTERNAL ID", "externalId=${authenticatedUser.externalId}\nfound=${existingUser.isPresent}")
        val existingEmailUser = if (existingUser.isPresent) {
            null
        } else {
            trace("LOOKUP BY EMAIL", "email=${authenticatedUser.email}")
            userRepository.findByEmail(authenticatedUser.email).orElse(null)
        }
        trace("LOOKUP BY EMAIL RESULT", "email=${authenticatedUser.email}\nfound=${existingEmailUser != null}")

        val user = when {
            existingUser.isPresent -> existingUser.get().apply {
                email = authenticatedUser.email
                firstName = authenticatedUser.firstName
                lastName = authenticatedUser.lastName
            }
            existingEmailUser != null -> existingEmailUser.apply {
                externalId = authenticatedUser.externalId
                email = authenticatedUser.email
                firstName = authenticatedUser.firstName
                lastName = authenticatedUser.lastName
                role = authenticatedUser.primaryRole
            }
            else -> User(
                externalId = authenticatedUser.externalId,
                email = authenticatedUser.email,
                firstName = authenticatedUser.firstName,
                lastName = authenticatedUser.lastName,
                role = authenticatedUser.primaryRole
            )
        }

        trace("PERSIST USER", "id=${user.id ?: "new"}\nexternalId=${user.externalId}\nemail=${user.email}")

        val saved = userRepository.save(user).toDto()
        trace("SYNC COMPLETE", "userId=${saved.id}\nemail=${saved.email}\ndepartments=${saved.departments.size}")
        return saved
    }

    @Transactional
    fun updateCurrentUser(externalId: String, request: UpdateUserRequest): UserDto {
        val user = userRepository.findByExternalIdWithDepartments(externalId)
            .orElseThrow { ResourceNotFoundException("User", externalId) }

        request.firstName?.let { user.firstName = it }
        request.lastName?.let { user.lastName = it }

        return userRepository.save(user).toDto()
    }

    @Transactional(readOnly = true)
    fun getUser(id: UUID): UserDto {
        val user = userRepository.findByIdWithDepartments(id)
            .orElseThrow { ResourceNotFoundException("User", id) }
        return user.toDto()
    }

    @Transactional(readOnly = true)
    fun listUsers(search: String?, pageable: Pageable): Page<UserSummaryDto> {
        val page = if (search.isNullOrBlank()) {
            userRepository.findAll(pageable)
        } else {
            userRepository.searchUsers(search, pageable)
        }
        return page.map { it.toSummaryDto() }
    }

    @Transactional
    fun updateUserRole(id: UUID, role: Role, currentUserRole: Role): UserDto {
        if (currentUserRole != Role.ADMIN) {
            throw AccessDeniedException("Only admins can update user roles")
        }

        val user = userRepository.findByIdWithDepartments(id)
            .orElseThrow { ResourceNotFoundException("User", id) }

        user.role = role
        return userRepository.save(user).toDto()
    }

    @Transactional(readOnly = true)
    fun getUsersByDepartment(departmentId: UUID, pageable: Pageable): Page<UserSummaryDto> {
        return userRepository.findByDepartmentId(departmentId, pageable).map { it.toSummaryDto() }
    }

    @Transactional(readOnly = true)
    fun getUserIdByExternalId(externalId: String): UUID {
        return userRepository.findByExternalId(externalId)
            .orElseThrow { ResourceNotFoundException("User", externalId) }
            .id!!
    }

    @Transactional(readOnly = true)
    fun getUserAccessContext(externalId: String): UserAccessContext {
        val user = userRepository.findByExternalIdWithDepartments(externalId)
            .orElseThrow { ResourceNotFoundException("User", externalId) }

        return UserAccessContext(
            userId = user.id!!,
            role = user.role,
            departmentIds = user.departments.mapNotNull { it.id }
        )
    }

    private fun User.toDto(): UserDto {
        return UserDto(
            id = this.id!!,
            externalId = this.externalId,
            email = this.email,
            firstName = this.firstName,
            lastName = this.lastName,
            role = this.role,
            departments = this.departments.map { it.toDepartmentDto() },
            createdAt = this.createdAt,
            updatedAt = this.updatedAt
        )
    }

    private fun User.toSummaryDto(): UserSummaryDto {
        return UserSummaryDto(
            id = this.id!!,
            email = this.email,
            firstName = this.firstName,
            lastName = this.lastName,
            role = this.role
        )
    }

    private fun com.docplatform.userservice.entity.Department.toDepartmentDto(): DepartmentDto {
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
