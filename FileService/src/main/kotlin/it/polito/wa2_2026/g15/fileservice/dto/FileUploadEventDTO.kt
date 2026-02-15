package it.polito.wa2_2026.g15.fileservice.dto

import java.util.UUID

/**
 * Questo DTO è specifico per l'evento Kafka.
 * Contiene SOLO i dati tecnici necessari all'AI Service per scaricare ed elaborare il file.
 */
data class FileUploadEventDTO(
    val fileId: UUID,
    val fileVersionId: Int, // Fondamentale per scaricare la versione giusta
    val ownerId: UUID,
    val filename: String,
    val contentType: String,
    val size: Long
)