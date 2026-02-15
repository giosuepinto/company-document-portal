package it.polito.wa2_2026.g15.fileservice.dto

import io.swagger.v3.oas.annotations.media.Schema
import it.polito.wa2_2026.g15.fileservice.entities.SharePermission
import java.util.UUID

/**
 * DTO for the request body when creating a share.
 */
@Schema(description = "Request payload to share a file with another user.")
data class CreateShareRequest(
    @Schema(description = "UUID of the target user who will receive access (must exist in UserDetailService)", example = "123e4567-e89b-12d3-a456-426614174000")
    val userId: UUID,

    @Schema(description = "Permission level to grant", example = "READ", implementation = SharePermission::class)
    val permission: SharePermission
)

/**
 * DTO for the response when returning share details.
 */
@Schema(description = "Details of a file sharing record.")
data class ShareDTO(
    @Schema(description = "Unique identifier of the share record", example = "987fcdeb-51a2-43d1-a456-426614174999")
    val id: UUID,

    @Schema(description = "UUID of the shared file", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")
    val fileId: UUID,

    @Schema(description = "UUID of the user who has access", example = "123e4567-e89b-12d3-a456-426614174000")
    val sharedWithUserId: UUID,

    @Schema(description = "Granted permission level", example = "WRITE")
    val permission: SharePermission
)