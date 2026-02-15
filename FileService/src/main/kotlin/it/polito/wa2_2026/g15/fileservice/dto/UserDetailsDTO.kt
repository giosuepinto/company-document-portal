package it.polito.wa2_2026.g15.fileservice.dto

import java.util.UUID

data class UserDetailsDTO(
    val id: UUID,
    val email: String,
    val role: String,
    val department: String? // <--- Questo è il campo fondamentale per l'ACL
)