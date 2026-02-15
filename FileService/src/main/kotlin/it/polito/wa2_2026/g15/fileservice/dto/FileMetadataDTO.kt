package it.polito.wa2_2026.g15.fileservice.dto

import io.swagger.v3.oas.annotations.media.Schema
import it.polito.wa2_2026.g15.fileservice.entities.UploadStatus
import java.time.Instant
import java.util.UUID

/**
 * DTO for representing file metadata in lists (e.g., paginated results).
 */
@Schema(description = "Details of a persistent file including metadata and status.")
data class FileMetadataDTO(
    @Schema(description = "Unique identifier of the file", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")
    val id: UUID,

    @Schema(description = "Name of the file", example = "project_presentation.pdf")
    val filename: String,

    @Schema(description = "MIME type of the file content", example = "application/pdf")
    val contentType: String,

    @Schema(description = "Size of the file in bytes", example = "2048576")
    val size: Long,

    @Schema(description = "Current status of the upload", example = "COMPLETED")
    val status: UploadStatus,

    @Schema(description = "Timestamp when the file was first created", example = "2025-11-20T10:15:30Z")
    val createdAt: Instant,

    @Schema(description = "UUID of the file owner", example = "123e4567-e89b-12d3-a456-426614174000")
    val owner: String,

    @Schema(description = "Number of versions available for this file", example = "3")
    val versionCount: Int,

    val permission: String,

    val summary: String?,
    val tags: List<String>?,
    val sensitivity: String?
)