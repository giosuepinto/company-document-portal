package it.polito.wa2.lab03.userdetailservice.dto

import it.polito.wa2.lab03.userdetailservice.entities.User
import java.time.Instant

/**
 * Converte un'entità User in un UserDTO (Risposta).
 */
fun User.toDTO(): UserDTO {
    return UserDTO(
        id = this.id!!, // L'ID viene generato al save, quindi è sicuro (se save è andato a buon fine)
        fullName = this.fullName,
        email = this.email,
        organization = this.organization,
        role = this.role,
        department = this.department,
        storageQuota = this.storageQuota,
        notes = this.notes,
        // FIX: Usiamo Elvis operator per evitare NullPointerException se Hibernate non ha ancora settato i timestamp
        createdAt = this.createdAt ?: Instant.now(),
        updatedAt = this.updatedAt ?: Instant.now()
    )
}

fun User.toShortDTO(): UserShortDTO {
    return UserShortDTO(
        id = this.id!!, // L'ID viene generato al save, quindi è sicuro (se save è andato a buon fine)
        fullName = this.fullName,
        email = this.email,
    )
}

fun UserRequestDTO.toEntity(): User {
    val user = User()

    user.id = this.id
        ?: throw IllegalArgumentException("User ID (UUID da Keycloak) è obbligatorio per il provisioning manuale.")
    user.fullName = this.fullName
    user.email = this.email
    user.organization = this.organization
    user.role = this.role
    user.department = this.department
    user.storageQuota = this.storageQuota
    user.notes = this.notes ?: "Manually created by the admin."

    return user
}