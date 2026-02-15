package it.polito.wa2_2026.g15.fileservice.services

import it.polito.wa2_2026.g15.fileservice.entities.File
import it.polito.wa2_2026.g15.fileservice.entities.FileShare
import it.polito.wa2_2026.g15.fileservice.entities.SharePermission
import it.polito.wa2_2026.g15.fileservice.exceptions.FileAlreadySharedException
import it.polito.wa2_2026.g15.fileservice.exceptions.FileNotFoundException
import it.polito.wa2_2026.g15.fileservice.exceptions.PermissionDeniedException
import it.polito.wa2_2026.g15.fileservice.exceptions.TargetUserNotFoundException
import it.polito.wa2_2026.g15.fileservice.repositories.FileRepository
import it.polito.wa2_2026.g15.fileservice.repositories.FileShareRepository
import it.polito.wa2_2026.g15.fileservice.utils.SecurityUtils
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class FileShareServiceImpl(
    private val fileRepository: FileRepository,
    private val fileShareRepository: FileShareRepository,
    private val userClient: UserClient,
    private val securityUtils: SecurityUtils
) {
    private val logger = LoggerFactory.getLogger(FileShareServiceImpl::class.java)

    private fun checkOwnerOrAdmin(file: File) {
        val currentUser = securityUtils.getCurrentUser()
        if (currentUser.isAdmin()) return
        if (file.owner == currentUser.id) return

        throw PermissionDeniedException("Only the file owner or admin can manage shares.")
    }

    /**
     * Creates a new share entry for a file.
     * Verifies that both the file and the target user exist.
     */
    @Transactional
    fun shareFile(fileId: UUID, targetUserId: UUID, permission: SharePermission): FileShare {
        logger.info("Sharing file {} with user {} (Permission: {})", fileId, targetUserId, permission)

        // 1. Verify file exists
        val file = fileRepository.findById(fileId)
            .orElseThrow {
                logger.warn("Share failed: File {} not found", fileId)
                FileNotFoundException(fileId)
            }

        checkOwnerOrAdmin(file)

        if (file.owner == targetUserId ) {
            throw FileAlreadySharedException(fileId, targetUserId)
        }

        // 2. Verify target user exists (Inter-service communication)
        if (!userClient.userExists(targetUserId)) {
            logger.warn("Share failed: User {} not found in UserDetailService", targetUserId)
            throw TargetUserNotFoundException(targetUserId)
        }

        // 3. Check duplicates
        if (fileShareRepository.existsByFileIdAndSharedWithUserId(fileId, targetUserId)) {
            logger.warn("Share failed: Already shared")
            throw FileAlreadySharedException(fileId, targetUserId)
        }

        // 4. Save
        val share = FileShare().apply {
            this.file = file
            this.sharedWithUserId = targetUserId
            this.permission = permission
        }

        val saved = fileShareRepository.save(share)
        logger.info("Share created successfully. Share ID: {}", saved.id)
        return saved
    }

    fun getSharesForFile(fileId: UUID): List<FileShare> {
        val file = fileRepository.findById(fileId)
            .orElseThrow { FileNotFoundException(fileId) }

        checkOwnerOrAdmin(file)
        return fileShareRepository.findByFileId(fileId)
    }

    fun getFilesSharedWithUser(userId: UUID): List<FileShare> {
        if (!userClient.userExists(userId)) {
            logger.warn("Share failed: User {} not found in UserDetailService", userId)
            throw TargetUserNotFoundException(userId)
        }

        val currentUser = securityUtils.getCurrentUser()
        if (!currentUser.isAdmin() && currentUser.id != userId) {
            throw PermissionDeniedException("You can only view files shared with yourself.")
        }

        return fileShareRepository.findBySharedWithUserId(userId)
    }

    @Transactional
    fun removeShare(fileId: UUID, shareId: UUID) {
        logger.info("Revoking share {} for file {}", shareId, fileId)

        val share = fileShareRepository.findById(shareId)
            .orElseThrow { IllegalArgumentException("Share not found") }

        if (share.file?.id != fileId) {
            logger.warn("Revoke failed: Share {} does not belong to file {}", shareId, fileId)
            throw IllegalArgumentException("Share mismatch")
        }

        checkOwnerOrAdmin(share.file!!)

        fileShareRepository.delete(share)
    }
}