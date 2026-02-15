package it.polito.wa2_2026.g15.fileservice.utils

import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class SecurityUtils {

    // Restituisce l'utente corrente parsato dal Token JWT
    fun getCurrentUser(): AuthenticatedUser {
        val authentication = SecurityContextHolder.getContext().authentication
        val principal = authentication?.principal

        if (principal is Jwt) {
            // Keycloak usa il campo 'sub' come UUID dell'utente
            val userId = UUID.fromString(principal.subject)

            // I ruoli sono stati mappati dal tuo KeycloakRealmRoleConverter in "ROLE_..."
            val roles = authentication.authorities.map { it.authority }

            return AuthenticatedUser(userId, roles)
        }
        throw AccessDeniedException("Utente non autenticato o token non valido")
    }

    data class AuthenticatedUser(val id: UUID, val roles: List<String>) {
        fun isAdmin() = roles.contains("ROLE_admin")
        fun isManager() = roles.contains("ROLE_manager")
        fun isStaff() = roles.contains("ROLE_staff")
    }
}