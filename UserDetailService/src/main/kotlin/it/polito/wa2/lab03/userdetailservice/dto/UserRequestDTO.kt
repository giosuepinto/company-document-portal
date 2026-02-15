package it.polito.wa2.lab03.userdetailservice.dto

import io.swagger.v3.oas.annotations.media.Schema
import it.polito.wa2.lab03.userdetailservice.entities.Role
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.PositiveOrZero
import jakarta.validation.constraints.Size
import java.util.UUID

@Schema(description = "Payload required to create or update a user.")
data class UserRequestDTO(
    @Schema(description = "The UUID of the user as provided by Keycloak. Required for new user creation.", example = "12345678-abcd-1234-abcd-1234567890ab")
    @field:NotNull(message = "User ID (UUID from Keycloak) must be provided for manual provisioning.")
    val id: UUID?,

    @Schema(description = "Full legal name (max 255 chars)", example = "Mario Rossi")
    @field:NotBlank(message = "Full name cannot be blank")
    @field:Size(max = 255)
    val fullName: String,

    @Schema(description = "Valid business email address", example = "mario.rossi@polito.it")
    @field:NotBlank(message = "Email cannot be blank")
    @field:Email(message = "Must be a valid email format")
    val email: String,

    @Schema(description = "Organization name", example = "Politecnico di Torino")
    @field:NotBlank(message = "Organization cannot be blank")
    val organization: String,

    @Schema(description = "Role to be assigned", example = "EDITOR")
    @field:NotNull(message = "Role must be specified")
    val role: Role,

    // Campi opzionali
    @Schema(description = "Department name", example = "IT Department", nullable = true)
    val department: String? = null,

    @Schema(description = "Storage quota in bytes (must be positive)", example = "5368709120", nullable = true)
    @field:PositiveOrZero(message = "Storage quota must be a non-negative number")
    val storageQuota: Long? = null,

    @Schema(description = "Administrative notes", example = "Temporary contractor", nullable = true)
    val notes: String? = null
)
