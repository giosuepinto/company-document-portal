package it.polito.wa2_2026.g15.fileservice.dto

data class QueryQuestionDTO(
    val question: String,
    // filters
    val fileType: String? = null,
    val specificFileIds: List<String>? = null
)