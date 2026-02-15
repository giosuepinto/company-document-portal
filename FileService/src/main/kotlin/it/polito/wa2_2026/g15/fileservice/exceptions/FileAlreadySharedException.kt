package it.polito.wa2_2026.g15.fileservice.exceptions

import java.util.UUID

// Lanciata quando provi a condividere un file già condiviso con lo stesso utente
class FileAlreadySharedException(fileId: UUID, userId: UUID) : RuntimeException("File $fileId is already shared with user $userId")