package com.docplatform.userservice.controller

import com.docplatform.common.dto.*
import com.docplatform.security.model.AuthenticatedUser
import com.docplatform.userservice.service.UserService
import org.slf4j.LoggerFactory
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/users")
class UserController(
    private val userService: UserService
) {
    private val logger = LoggerFactory.getLogger(UserController::class.java)

    private fun trace(title: String, message: String) {
        logger.info("""


==================== USER CONTROLLER: $title ====================
$message
===============================================================


""".trimIndent())
    }

    @GetMapping("/me")
    fun getCurrentUser(@AuthenticationPrincipal jwt: Jwt): ResponseEntity<ApiResponse<UserDto>> {
        trace("GET /ME START", "subject=${jwt.subject}\nemail=${jwt.getClaimAsString("email")}")
        val authenticatedUser = AuthenticatedUser.fromJwt(jwt)
        trace("SYNC USER CALL", "externalId=${authenticatedUser.externalId}\nemail=${authenticatedUser.email}")
        val user = userService.syncUser(authenticatedUser)
        trace("GET /ME END", "userId=${user.id}\nemail=${user.email}")
        return ResponseEntity.ok(ApiResponse.success(user))
    }

    @PutMapping("/me")
    fun updateCurrentUser(
        @AuthenticationPrincipal jwt: Jwt,
        @Valid @RequestBody request: UpdateUserRequest
    ): ResponseEntity<ApiResponse<UserDto>> {
        val user = userService.updateCurrentUser(jwt.subject, request)
        return ResponseEntity.ok(ApiResponse.success(user))
    }

    @GetMapping("/{id}")
    fun getUser(@PathVariable id: UUID): ResponseEntity<ApiResponse<UserDto>> {
        val user = userService.getUser(id)
        return ResponseEntity.ok(ApiResponse.success(user))
    }

    @GetMapping
    fun listUsers(
        @RequestParam(required = false) search: String?,
        @PageableDefault(size = 20) pageable: Pageable
    ): ResponseEntity<ApiResponse<PagedResponse<UserSummaryDto>>> {
        val page = userService.listUsers(search, pageable)
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

    @PutMapping("/{id}/role")
    fun updateUserRole(
        @PathVariable id: UUID,
        @RequestBody request: UpdateUserRequest,
        @AuthenticationPrincipal jwt: Jwt
    ): ResponseEntity<ApiResponse<UserDto>> {
        val currentUser = AuthenticatedUser.fromJwt(jwt)
        val user = userService.updateUserRole(id, request.role!!, currentUser.primaryRole)
        return ResponseEntity.ok(ApiResponse.success(user))
    }

    @GetMapping("/roles")
    fun listRoles(): ResponseEntity<ApiResponse<List<Role>>> {
        return ResponseEntity.ok(ApiResponse.success(Role.entries))
    }
}
