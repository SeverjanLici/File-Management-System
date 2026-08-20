package com.docplatform.userservice.controller

import com.docplatform.common.dto.*
import com.docplatform.security.model.AuthenticatedUser
import com.docplatform.userservice.service.DepartmentService
import com.docplatform.userservice.service.UserService
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/departments")
class DepartmentController(
    private val departmentService: DepartmentService,
    private val userService: UserService
) {

    @PostMapping
    fun createDepartment(
        @Valid @RequestBody request: CreateDepartmentRequest,
        @AuthenticationPrincipal jwt: Jwt
    ): ResponseEntity<ApiResponse<DepartmentDto>> {
        val currentUser = AuthenticatedUser.fromJwt(jwt)
        val department = departmentService.createDepartment(request, currentUser.primaryRole)
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success(department))
    }

    @GetMapping("/{id}")
    fun getDepartment(@PathVariable id: UUID): ResponseEntity<ApiResponse<DepartmentDto>> {
        val department = departmentService.getDepartment(id)
        return ResponseEntity.ok(ApiResponse.success(department))
    }

    @GetMapping
    fun listDepartments(
        @RequestParam(required = false) search: String?,
        @PageableDefault(size = 20) pageable: Pageable
    ): ResponseEntity<ApiResponse<PagedResponse<DepartmentDto>>> {
        val page = departmentService.listDepartments(search, pageable)
        val pagedResponse = PagedResponse(
            content = page.content,
            page = page.number,
            size = page.size,
            totalElements = page.totalElements,
            totalPages = page.totalPages,
            hasNext = page.hasNext(),
            hasPrevious = page.hasPrevious()
        )
        return ResponseEntity.ok(ApiResponse.success(pagedResponse))
    }

    @PutMapping("/{id}")
    fun updateDepartment(
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateDepartmentRequest,
        @AuthenticationPrincipal jwt: Jwt
    ): ResponseEntity<ApiResponse<DepartmentDto>> {
        val currentUser = AuthenticatedUser.fromJwt(jwt)
        val department = departmentService.updateDepartment(id, request, currentUser.primaryRole)
        return ResponseEntity.ok(ApiResponse.success(department))
    }

    @DeleteMapping("/{id}")
    fun deleteDepartment(
        @PathVariable id: UUID,
        @AuthenticationPrincipal jwt: Jwt
    ): ResponseEntity<Void> {
        val currentUser = AuthenticatedUser.fromJwt(jwt)
        departmentService.deleteDepartment(id, currentUser.primaryRole)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/{id}/members")
    fun addMember(
        @PathVariable id: UUID,
        @Valid @RequestBody request: DepartmentMemberRequest,
        @AuthenticationPrincipal jwt: Jwt
    ): ResponseEntity<ApiResponse<DepartmentDto>> {
        val currentUser = AuthenticatedUser.fromJwt(jwt)
        val department = departmentService.addMember(
            id,
            request.userId,
            currentUser.primaryRole,
            currentUser.externalId
        )
        return ResponseEntity.ok(ApiResponse.success(department))
    }

    @DeleteMapping("/{id}/members/{userId}")
    fun removeMember(
        @PathVariable id: UUID,
        @PathVariable userId: UUID,
        @AuthenticationPrincipal jwt: Jwt
    ): ResponseEntity<ApiResponse<DepartmentDto>> {
        val currentUser = AuthenticatedUser.fromJwt(jwt)
        val department = departmentService.removeMember(
            id,
            userId,
            currentUser.primaryRole,
            currentUser.externalId
        )
        return ResponseEntity.ok(ApiResponse.success(department))
    }

    @GetMapping("/{id}/members")
    fun getDepartmentMembers(
        @PathVariable id: UUID,
        @PageableDefault(size = 20) pageable: Pageable
    ): ResponseEntity<ApiResponse<PagedResponse<UserSummaryDto>>> {
        val page = userService.getUsersByDepartment(id, pageable)
        val pagedResponse = PagedResponse(
            content = page.content,
            page = page.number,
            size = page.size,
            totalElements = page.totalElements,
            totalPages = page.totalPages,
            hasNext = page.hasNext(),
            hasPrevious = page.hasPrevious()
        )
        return ResponseEntity.ok(ApiResponse.success(pagedResponse))
    }
}
