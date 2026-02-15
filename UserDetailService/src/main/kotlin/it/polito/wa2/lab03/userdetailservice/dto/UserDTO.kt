package it.polito.wa2.lab03.userdetailservice.dto

import io.swagger.v3.oas.annotations.media.Schema
import it.polito.wa2.lab03.userdetailservice.entities.Role
import java.time.Instant
import java.util.UUID

@Schema(description = "Response object containing full user details.")
data class UserDTO(
    @Schema(description = "Unique identifier of the user", example = "123e4567-e89b-12d3-a456-426614174000")
    val id: UUID,

    @Schema(description = "Full legal name of the user", example = "Mario Rossi")
    val fullName: String,

    @Schema(description = "Business email address", example = "mario.rossi@polito.it")
    val email: String,

    @Schema(description = "Organization or Company name", example = "Politecnico di Torino")
    val organization: String,

    @Schema(description = "Assigned role within the system", example = "EDITOR")
    val role: Role,

    @Schema(description = "Department or Business Unit", example = "Computer Engineering", nullable = true)
    val department: String?,

    @Schema(description = "Allocated storage quota in bytes", example = "10737418240", nullable = true)
    val storageQuota: Long?,

    @Schema(description = "Optional administrative notes", example = "Project manager for Lab03", nullable = true)
    val notes: String?,

    @Schema(description = "Timestamp of account creation", example = "2025-10-01T12:00:00Z")
    val createdAt: Instant,

    @Schema(description = "Timestamp of last profile update", example = "2025-10-05T15:30:00Z")
    val updatedAt: Instant
)
