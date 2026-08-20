package com.docplatform.documentservice.service

import com.docplatform.common.dto.Role
import com.docplatform.common.dto.UserAccessContext
import com.docplatform.security.model.AuthenticatedUser
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import java.util.UUID

data class UserContext(
    val userId: UUID,
    val userName: String,
    val role: Role,
    val departmentIds: List<UUID>
)

@Service
class UserContextService(
    @Value("\${services.user-service.url:http://localhost:8081}")
    private val userServiceUrl: String
) {
    private val restTemplate = RestTemplate()

    fun getUserContext(jwt: Jwt): UserContext {
        val authenticatedUser = AuthenticatedUser.fromJwt(jwt)

        return try {
            val response = restTemplate.getForObject(
                "$userServiceUrl/api/v1/internal/users/${jwt.subject}/context",
                UserAccessContextResponse::class.java
            )

            if (response?.data != null) {
                UserContext(
                    userId = response.data.userId,
                    userName = "${authenticatedUser.firstName} ${authenticatedUser.lastName}",
                    role = response.data.role,
                    departmentIds = response.data.departmentIds
                )
            } else {
                // Fallback to JWT-based context if user service unavailable
                createFallbackContext(authenticatedUser)
            }
        } catch (e: Exception) {
            // Fallback to JWT-based context if user service unavailable
            createFallbackContext(authenticatedUser)
        }
    }

    private fun createFallbackContext(user: AuthenticatedUser): UserContext {
        return UserContext(
            userId = UUID.nameUUIDFromBytes(user.externalId.toByteArray()),
            userName = user.fullName,
            role = user.primaryRole,
            departmentIds = emptyList()
        )
    }
}

data class UserAccessContextResponse(
    val success: Boolean,
    val data: UserAccessContext?
)
