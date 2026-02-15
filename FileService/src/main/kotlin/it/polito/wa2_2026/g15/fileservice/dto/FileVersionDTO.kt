package it.polito.wa2_2026.g15.fileservice.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

@Schema(description = "Details of a specific version of a file.")
data class FileVersionDTO(
    val version: Int,
    val filename: String,
    val contentType: String,
    val size: Long,
    val updatedAt: Instant
)