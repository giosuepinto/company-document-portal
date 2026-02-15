package it.polito.wa2_2026.g15.fileservice.repositories

import it.polito.wa2_2026.g15.fileservice.entities.Outbox
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface OutboxRepository: JpaRepository<Outbox, UUID> {
}