package it.polito.wa2_2026.g15.aiprocessingservice.dto

data class RagQueryRequestDTO (
    val question: String,
    val filterFileIds: List<String> // UUID list as strings
)