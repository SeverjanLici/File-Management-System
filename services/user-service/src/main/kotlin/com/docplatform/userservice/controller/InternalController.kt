package com.docplatform.userservice.controller

import com.docplatform.common.dto.ApiResponse
import com.docplatform.common.dto.UserAccessContext
import com.docplatform.userservice.service.UserService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/internal")
class InternalController(
    private val userService: UserService
) {

    @GetMapping("/users/{externalId}/context")
    fun getUserAccessContext(@PathVariable externalId: String): ResponseEntity<ApiResponse<UserAccessContext>> {
        val context = userService.getUserAccessContext(externalId)
        return ResponseEntity.ok(ApiResponse.success(context))
    }

    @GetMapping("/users/by-external-id/{externalId}")
    fun getUserIdByExternalId(@PathVariable externalId: String): ResponseEntity<ApiResponse<UUID>> {
        val userId = userService.getUserIdByExternalId(externalId)
        return ResponseEntity.ok(ApiResponse.success(userId))
    }
}
