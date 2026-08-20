package com.docplatform.security.util

import com.docplatform.security.model.AuthenticatedUser
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken

object SecurityUtils {

    fun getCurrentJwt(): Jwt? {
        val authentication = SecurityContextHolder.getContext().authentication
        if (authentication is JwtAuthenticationToken) {
            return authentication.token
        }
        return null
    }

    fun getCurrentUser(): AuthenticatedUser? {
        return getCurrentJwt()?.let { AuthenticatedUser.fromJwt(it) }
    }

    fun getCurrentUserOrThrow(): AuthenticatedUser {
        return getCurrentUser() ?: throw IllegalStateException("No authenticated user found")
    }

    fun getCurrentExternalId(): String? {
        return getCurrentJwt()?.subject
    }

    fun getCurrentExternalIdOrThrow(): String {
        return getCurrentExternalId() ?: throw IllegalStateException("No authenticated user found")
    }

    fun hasRole(role: String): Boolean {
        val authentication = SecurityContextHolder.getContext().authentication
        return authentication?.authorities?.any {
            it.authority == "ROLE_$role" || it.authority == role
        } ?: false
    }

    fun isAdmin(): Boolean = hasRole("ADMIN")

    fun isManager(): Boolean = hasRole("MANAGER")

    fun isUser(): Boolean = hasRole("USER")
}
