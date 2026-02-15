package it.polito.wa2_2026.g15.fileservice.repositories

import it.polito.wa2_2026.g15.fileservice.entities.FileShare
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

/**
 * Repository for FileShare entity operations.
 */
interface FileShareRepository : JpaRepository<FileShare, UUID> {

    // Find all shares associated with a specific file ID
    fun findByFileId(fileId: UUID): List<FileShare>

    // Find all files shared with a specific user ID
    fun findBySharedWithUserId(userId: UUID): List<FileShare>

    // Check if a specific share already exists to prevent duplicates
    fun existsByFileIdAndSharedWithUserId(fileId: UUID, userId: UUID): Boolean
}