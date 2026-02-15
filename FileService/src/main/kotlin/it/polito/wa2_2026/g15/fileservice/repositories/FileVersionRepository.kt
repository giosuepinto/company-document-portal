package it.polito.wa2_2026.g15.fileservice.repositories

import it.polito.wa2_2026.g15.fileservice.entities.File
import it.polito.wa2_2026.g15.fileservice.entities.FileVersion
import it.polito.wa2_2026.g15.fileservice.entities.FileVersionId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface FileVersionRepository : JpaRepository<FileVersion, FileVersionId> {
    // Ritorna la versione più recente (by updatedAt) per un file
    fun findTopByFileOrderByUpdatedAtDesc(file: File): FileVersion?
    fun findTopByIdFileIdOrderByUpdatedAtDesc(fileId: UUID): FileVersion?
    // Lista ordinata per versione decrescente (v3, v2, v1...)
    fun findAllByIdFileIdOrderByIdVersionDesc(fileId: UUID): List<FileVersion>

    // Trova una versione specifica
    fun findByIdFileIdAndIdVersion(fileId: UUID, version: Int): FileVersion?
}
