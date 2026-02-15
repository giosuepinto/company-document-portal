package it.polito.wa2_2026.g15.fileservice.exceptions

import java.util.UUID

// Lanciata quando provi a condividere con un utente che il UserClient dice non esistere
class TargetUserNotFoundException(userId: UUID) : RuntimeException("Target user with ID $userId does not exist")
