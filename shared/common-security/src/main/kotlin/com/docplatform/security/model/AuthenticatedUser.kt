package com.docplatform.security.model

import com.docplatform.common.dto.Role
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import java.util.UUID

data class AuthenticatedUser(
    val externalId: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val roles: List<String>
) {
    val fullName: String
        get() = "$firstName $lastName"

    val primaryRole: Role
        get() = when {
            roles.any { it.equals("ADMIN", ignoreCase = true) } -> Role.ADMIN
            roles.any { it.equals("MANAGER", ignoreCase = true) } -> Role.MANAGER
            else -> Role.USER
        }

    companion object {
        fun fromJwt(jwt: Jwt): AuthenticatedUser {
            val externalId = jwt.subject
            val email = jwt.getClaimAsString("email") ?: ""
            val firstName = jwt.getClaimAsString("given_name") ?: jwt.getClaimAsString("name")?.split(" ")?.firstOrNull() ?: ""
            val lastName = jwt.getClaimAsString("family_name") ?: jwt.getClaimAsString("name")?.split(" ")?.lastOrNull() ?: ""

            val roles = mutableListOf<String>()

            // Extract realm roles from Keycloak realm_access claim
            val realmAccess = jwt.getClaim<Map<String, Any>>("realm_access")
            val realmRoles = realmAccess?.get("roles") as? List<*>
            realmRoles?.filterIsInstance<String>()?.let { roles.addAll(it) }

            return AuthenticatedUser(
                externalId = externalId,
                email = email,
                firstName = firstName,
                lastName = lastName,
                roles = roles.ifEmpty { listOf("USER") }
            )
        }

        fun current(): AuthenticatedUser? {
            val authentication = SecurityContextHolder.getContext().authentication
            if (authentication is JwtAuthenticationToken) {
                return fromJwt(authentication.token)
            }
            return null
        }

        fun currentOrThrow(): AuthenticatedUser {
            return current() ?: throw IllegalStateException("No authenticated user found")
        }
    }
}
