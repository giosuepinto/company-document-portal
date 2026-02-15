package it.polito.wa2_2026.g15.fileservice.dto

// Import the validation annotation
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size

/**
 * DTO for updating file metadata via PATCH.
 * All fields are optional.
 */
@Schema(description = "Payload for updating file metadata (PATCH). All fields are optional.")
data class FileUpdateDTO(

    @Schema(description = "New filename (must not be empty if provided)", example = "final_report_v2.pdf", nullable = true)
    @field:Size(min = 1, message = "Filename cannot be empty")
    val filename: String?,

    @Schema(description = "New MIME type (must not be empty if provided)", example = "application/pdf", nullable = true)
    @field:Size(min = 1, message = "Content type cannot be empty")
    val contentType: String?
)