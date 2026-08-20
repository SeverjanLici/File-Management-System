package com.docplatform.security.config

import org.springframework.core.convert.converter.Converter
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt

class KeycloakGrantedAuthoritiesConverter : Converter<Jwt, Collection<GrantedAuthority>> {

    override fun convert(jwt: Jwt): Collection<GrantedAuthority> {
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
