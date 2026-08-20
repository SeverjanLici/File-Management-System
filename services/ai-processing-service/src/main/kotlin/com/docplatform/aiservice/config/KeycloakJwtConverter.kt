package com.docplatform.aiservice.config

import org.springframework.core.convert.converter.Converter
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken

class KeycloakJwtConverter : Converter<Jwt, AbstractAuthenticationToken> {

    override fun convert(jwt: Jwt): AbstractAuthenticationToken {
        val authorities = extractAuthorities(jwt)
        return JwtAuthenticationToken(jwt, authorities, jwt.subject)
    }

    private fun extractAuthorities(jwt: Jwt): Collection<GrantedAuthority> {
        val authorities = mutableListOf<GrantedAuthority>()

        // Keycloak puts realm roles in realm_access.roles
        val realmAccess = jwt.getClaim<Map<String, Any>>("realm_access")
        val realmRoles = realmAccess?.get("roles") as? List<*>
        realmRoles?.filterIsInstance<String>()?.forEach { role ->
            authorities.add(SimpleGrantedAuthority("ROLE_${role.uppercase()}"))
        }

        if (authorities.isEmpty()) {
            authorities.add(SimpleGrantedAuthority("ROLE_USER"))
        }

        return authorities
    }
}
