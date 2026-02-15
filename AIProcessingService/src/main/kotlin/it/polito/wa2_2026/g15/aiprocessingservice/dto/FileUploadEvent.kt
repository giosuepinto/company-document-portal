package it.polito.wa2_2026.g15.aiprocessingservice.dto

import java.util.UUID

/**
 * Evento ricevuto da Kafka quando un file viene caricato.
 * Contiene le coordinate per scaricare il file dal FileService.
 */
data class FileUploadEvent(
    val fileId: UUID,
    val fileVersionId: Int,
    val ownerId: UUID,
    val filename: String,
    val contentType: String,
    val size: Long
)