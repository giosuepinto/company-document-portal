package it.polito.wa2_2026.g15.fileservice.exceptions

import java.util.UUID

// Lanciata quando cerchi un File (entità logica) che non esiste
class FileNotFoundException(id: UUID) : RuntimeException("File with ID $id not found")
