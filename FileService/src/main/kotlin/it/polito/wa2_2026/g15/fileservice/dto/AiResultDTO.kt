package it.polito.wa2_2026.g15.fileservice.dto

data class AiResultDTO(
    val summary: String?,
    val tags: List<String> = emptyList(),
    val sensitivity: String?
)