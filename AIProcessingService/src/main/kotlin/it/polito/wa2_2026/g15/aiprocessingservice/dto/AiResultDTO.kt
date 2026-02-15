package it.polito.wa2_2026.g15.aiprocessingservice.dto

data class AiResultDTO(
    val summary: String?,
    val tags: List<String> = emptyList(),
    val sensitivity: String?
)