package it.polito.wa2_2026.g15.fileservice.security

import org.springframework.core.convert.converter.Converter
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt

// Estrae i ruoli dal claim "realm_access" del token Keycloak
class KeycloakRealmRoleConverter : Converter<Jwt, Collection<GrantedAuthority>> {
    override fun convert(jwt: Jwt): Collection<GrantedAuthority> {
        val realmAccess = jwt.getClaim<Map<String, Any>>("realm_access")

        if (realmAccess == null || realmAccess.isEmpty()) {
            return emptyList()
        }

        @Suppress("UNCHECKED_CAST")
        val roles = realmAccess["roles"] as? List<String> ?: return emptyList()

        // Aggiunge il prefisso "ROLE_" che piace a Spring (es. "ROLE_admin")
        return roles.map { role -> SimpleGrantedAuthority("ROLE_$role") }
    }
}