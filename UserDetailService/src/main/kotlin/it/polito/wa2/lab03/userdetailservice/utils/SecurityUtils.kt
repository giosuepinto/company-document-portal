package it.polito.wa2.lab03.userdetailservice.utils

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

    fun getCurrentPrincipal(): UserPrincipalDTO {
        val authentication = SecurityContextHolder.getContext().authentication
        val principal = authentication?.principal

        if (principal is Jwt) {
            val userId = UUID.fromString(principal.subject)

            // Estrazione dei claims standard del JWT (dipende dal tuo setup Keycloak)
            val userEmail = principal.getClaimAsString("email") ?: throw AccessDeniedException("Email claim mancante nel token")
            val userName = principal.getClaimAsString("name") ?: throw AccessDeniedException("Nome claim mancante nel token")

            val roles = authentication.authorities.map { it.authority }

            return UserPrincipalDTO(
                id = userId,
                email = userEmail,
                fullName = userName,
                roles = roles
            )
        }
        throw AccessDeniedException("Utente non autenticato o token non valido")
    }

    data class AuthenticatedUser(val id: UUID, val roles: List<String>) {
        // Controllo case-insensitive per evitare problemi con Keycloak (manager vs ROLE_MANAGER)
        fun isAdmin() = hasRole("ADMIN")
        fun isManager() = hasRole("MANAGER")
        fun isStaff() = hasRole("STAFF")

        private fun hasRole(roleName: String): Boolean {
            // Cerca "ROLE_NAME" o "name" ignorando case e prefissi
            return roles.any { it.equals("ROLE_$roleName", ignoreCase = true) || it.equals(roleName, ignoreCase = true) }
        }
    }

    data class UserPrincipalDTO(
        val id: UUID,
        val email: String, // Nuovo: Necessario per il Provisioning
        val fullName: String, // Nuovo: Necessario per il Provisioning
        val roles: List<String>
    ) {
        fun isAdmin() = roles.contains("ROLE_admin")
        fun isManager() = roles.contains("ROLE_manager")
        fun isStaff() = roles.contains("ROLE_staff")
    }

}