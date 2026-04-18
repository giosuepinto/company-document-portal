package it.polito.wa2.lab03.userdetailservice.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(description = "Response object containing short user details.")
data class UserShortDTO(
    @Schema(description = "Unique identifier of the user", example = "123e4567-e89b-12d3-a456-426614174000")
    val id: UUID,

    @Schema(description = "Full legal name of the user", example = "Mario Rossi")
    val fullName: String,

    @Schema(description = "Business email address", example = "mario.rossi@polito.it")
    val email: String,
)
