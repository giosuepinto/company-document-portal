package it.polito.wa2_2026.g15.fileservice.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.headers.Header
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import it.polito.wa2_2026.g15.fileservice.dto.AiResultDTO
import it.polito.wa2_2026.g15.fileservice.dto.CreateShareRequest
import it.polito.wa2_2026.g15.fileservice.dto.FileMetadataDTO
import it.polito.wa2_2026.g15.fileservice.dto.FileUpdateDTO
import it.polito.wa2_2026.g15.fileservice.dto.ShareDTO
import it.polito.wa2_2026.g15.fileservice.dto.FileVersionDTO
import it.polito.wa2_2026.g15.fileservice.dto.QueryQuestionDTO
import it.polito.wa2_2026.g15.fileservice.entities.SharePermission
import it.polito.wa2_2026.g15.fileservice.services.UploadService
import it.polito.wa2_2026.g15.fileservice.services.FileShareServiceImpl
import it.polito.wa2_2026.g15.fileservice.entities.UploadStatus
import it.polito.wa2_2026.g15.fileservice.utils.SecurityUtils
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.core.io.InputStreamResource
import org.springframework.core.io.Resource
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

// SECURITY IMPORTS
import org.springframework.security.access.prepost.PreAuthorize

@RestController
@RequestMapping("/api/v1/files")
@Tag(name = "File Management", description = "Operations for managing persistent files, versions, and sharing.")
class FileController(
    private val uploadService: UploadService,
    private val shareService: FileShareServiceImpl,
    private val securityUtils: SecurityUtils,
) {

    private val logger = LoggerFactory.getLogger(FileController::class.java)

    /**
     * Endpoint: GET /api/v1/files/{id}
     */
    @Operation(summary = "Get File Metadata", description = "Retrieves metadata for a persistent file.")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "File metadata retrieved successfully", content = [Content(schema = Schema(implementation = FileMetadataDTO::class))]),
        ApiResponse(responseCode = "404", description = "File not found"),
        ApiResponse(responseCode = "400", description = "Invalid UUID format")
    ])
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('staff', 'manager', 'admin', 'ROLE_AI_WORKER')")
    fun getFileDetails(
        @Parameter(description = "UUID of the file to retrieve", required = true)
        @PathVariable id: String
    ): ResponseEntity<FileMetadataDTO> {
        logger.info("REST: Request to get file details for {}", id)

        val fileId = try {
            UUID.fromString(id)
        } catch (_: IllegalArgumentException) {
            throw IllegalArgumentException("Invalid UUID format: $id")
        }

        // 1. Call Service (ACL check inside)
        val file = uploadService.getPersistentFile(fileId)
        val currentUser = securityUtils.getCurrentUser()

        // 2. Retrieve the latest version metadata
        val latestVersion = file.versions.maxByOrNull { it.updatedAt }

        // 3. Calcolo Permessi (Logica centralizzata)
        val perm = when {
            currentUser.isAdmin() -> "ADMIN"
            file.owner == currentUser.id -> "OWNER"
            file.sharingRules.any { it.sharedWithUserId == currentUser.id && it.permission == SharePermission.WRITE } -> "WRITE"
            // Se è un Manager e vede il file (che non è suo), assumiamo sia del dipartimento -> WRITE
            currentUser.isManager() -> "WRITE"
            else -> "READ"
        }

        // 4. Map to DTO
        val metadataDTO = FileMetadataDTO(
            id = file.id!!,
            filename = latestVersion?.filename ?: "unknown",
            contentType = latestVersion?.contentType ?: "application/octet-stream",
            size = latestVersion?.size ?: 0L,
            status = UploadStatus.COMPLETED,
            createdAt = file.createdAt,
            owner = file.owner.toString(),
            versionCount = file.versions.size,
            permission = perm, // <--- NUOVO CAMPO
            summary = latestVersion?.summary,
            tags = latestVersion?.tags,
            sensitivity = latestVersion?.sensitivity
        )

        return ResponseEntity.ok(metadataDTO)
    }

    /**
     * Endpoint: GET /api/v1/files/{id}/download
     * Requirement: Download the file content
     */
    @Operation(summary = "Download File", description = "Downloads the binary content of the latest version of the file.")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "File download stream", content = [Content(mediaType = "application/octet-stream")]),
        ApiResponse(responseCode = "404", description = "File not found"),
        ApiResponse(responseCode = "423", description = "File is locked (upload not complete)")
    ])
    @GetMapping("/{id}/download")
    @PreAuthorize("hasAnyRole('staff', 'manager', 'admin')") // RBAC
    fun downloadFile(
        @Parameter(description = "UUID of the file to download", required = true)
        @PathVariable id: String,
        @Parameter(description = "Action type: 'view' for inline display, otherwise attachment", required = false, example = "view")
        @RequestParam(required = false) action: String?
    ): ResponseEntity<InputStreamResource> {
        logger.info("REST: Request to download file {}. Action: {}", id, action)

        val uploadId = try {
            UUID.fromString(id)
        } catch (_: IllegalArgumentException) {
            throw IllegalArgumentException("Invalid UUID format: $id")
        }

        // Call service (ACL check inside)
        val (fileVersionEntry, inputStream) = uploadService.getDownloadStream(uploadId)

        // 2. Prepare resource
        val resource = InputStreamResource(inputStream)

        // 3. Set HTTP Headers
        val headers = HttpHeaders()
        headers.contentType = MediaType.parseMediaType(fileVersionEntry.contentType)
        headers.contentLength = fileVersionEntry.size

        val dispositionType = if (action == "view") "inline" else "attachment"
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "$dispositionType; filename=\"${fileVersionEntry.filename}\"")

        return ResponseEntity.ok().headers(headers).body(resource)
    }

    /**
     * Endpoint: GET /api/v1/files
     * Requirement: List PERSISTED files with pagination and optional OWNER filtering.
     */
    @Operation(summary = "List Files", description = "Retrieves a paginated list of all persistent files. Can be filtered by owner.")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "List of files retrieved successfully"),
        ApiResponse(responseCode = "400", description = "Invalid UUID format for owner filter")
    ])
    @GetMapping
    @PreAuthorize("hasAnyRole('staff', 'manager', 'admin')") // RBAC
    fun getFileList(
        @Parameter(description = "Filter files by Owner UUID", required = false)
        @RequestParam(required = false) owner: String?,
        @Parameter(description = "Pagination configuration (page, size, sort)", hidden = true) // Hidden because SpringDoc handles Pageable separately usually
        @PageableDefault(size = 10, sort = ["createdAt"], direction = Sort.Direction.DESC) pageable: Pageable
    ): ResponseEntity<Page<FileMetadataDTO>> {

        logger.info("REST: Request to list files. Owner filter: {}", owner ?: "ALL")

        val ownerUuid = try {
            if (owner != null) UUID.fromString(owner) else null
        } catch (_: IllegalArgumentException) {
            logger.warn("Invalid UUID format for owner filter: {}", owner)
            throw IllegalArgumentException("Invalid UUID format for owner: $owner")
        }

        // Call Service (ACL Filtering inside)
        val filePage = uploadService.getPersistentFiles(ownerUuid, pageable)

        val dtoPage = filePage.map { file ->
            val latestVersion = file.versions.maxByOrNull { it.updatedAt }

            // --- CALCOLO PERMESSI ---
            val perm = uploadService.checkWritePermissionString(file)
            // ------------------------

            FileMetadataDTO(
                id = file.id!!,
                filename = latestVersion?.filename ?: "unknown",
                contentType = latestVersion?.contentType ?: "application/octet-stream",
                size = latestVersion?.size ?: 0L,
                status = UploadStatus.COMPLETED,
                createdAt = file.createdAt,
                owner = file.owner.toString(),
                versionCount = file.versions.size,
                permission = perm, // <--- ASSEGNA IL PERMESSO
                summary = latestVersion?.summary,
                tags = latestVersion?.tags,
                sensitivity = latestVersion?.sensitivity
            )
        }
        return ResponseEntity.ok(dtoPage)
    }

    /**
     * Endpoint: PATCH /api/v1/files/{id}
     * Requirement: Partial metadata updates on the PERSISTENT file (latest version).
     */
    @Operation(summary = "Update Metadata", description = "Updates the metadata (filename, content-type) of the latest file version.")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Metadata updated successfully"),
        ApiResponse(responseCode = "404", description = "File not found"),
        ApiResponse(responseCode = "400", description = "Invalid input data or UUID"),
        ApiResponse(responseCode = "403", description = "Insufficient permissions")
    ])
    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('staff', 'manager', 'admin')")
    fun updateFileMetadata(
        @Parameter(description = "UUID of the file to update", required = true)
        @PathVariable id: String,
        @Valid @RequestBody updateDTO: FileUpdateDTO
    ): ResponseEntity<FileMetadataDTO> {
        logger.info("REST: Request to update metadata for file {}", id)

        val fileId = try {
            UUID.fromString(id)
        } catch (_: IllegalArgumentException) {
            throw IllegalArgumentException("Invalid UUID format: $id")
        }

        // 1. Esegui l'update nel service
        // Il service verifica le ACL (checkWritePermission) ed esegue la modifica.
        // Restituisce il DTO aggiornato (con permission impostato a "WRITE" di default).
        val partialDTO = uploadService.updatePersistentFileMetadata(fileId, updateDTO)

        // 2. Recupera l'utente corrente per raffinare il permesso
        val currentUser = securityUtils.getCurrentUser()

        // 3. Calcola il permesso specifico (ADMIN vs OWNER vs WRITE)
        // Se siamo arrivati qui, l'operazione è riuscita, quindi abbiamo ALMENO "WRITE".
        // Raffiniamo solo per dire al frontend se siamo Admin o Owner.
        val refinedPermission = when {
            currentUser.isAdmin() -> "ADMIN"
            partialDTO.owner == currentUser.id.toString() -> "OWNER"
            else -> "WRITE"
        }

        // 4. Restituisci una copia del DTO con il permesso corretto
        return ResponseEntity.ok(partialDTO.copy(permission = refinedPermission))
    }

    /**
     * Endpoint: DELETE /api/v1/files/{id}
     */
    @Operation(summary = "Delete File", description = "Deletes a file and all its versions/shares.")
    @ApiResponses(value = [
        ApiResponse(responseCode = "204", description = "File deleted successfully"),
        ApiResponse(responseCode = "404", description = "File not found"),
        ApiResponse(responseCode = "400", description = "Invalid UUID format")
    ])
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('staff', 'manager', 'admin')") // RBAC - Service checks if Owner/Admin
    fun deleteFile(
        @Parameter(description = "UUID of the file to delete", required = true)
        @PathVariable id: String
    ): ResponseEntity<Void> {
        logger.info("REST: Request to delete file {}", id)

        val uploadId = try {
            UUID.fromString(id)
        } catch (_: IllegalArgumentException) {
            throw IllegalArgumentException("Invalid UUID format: $id")
        }

        uploadService.terminateUpload(uploadId)

        return ResponseEntity.noContent().build()
    }

    /**
     * Endpoint: POST /api/v1/files/{id}/shares
     */
    @Operation(summary = "Share File", description = "Grants access to a file for a specific user.")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "File shared successfully"),
        ApiResponse(responseCode = "404", description = "File or Target User not found"),
        ApiResponse(responseCode = "409", description = "File is already shared with this user")
    ])
    @PostMapping("/{id}/shares")
    @PreAuthorize("hasAnyRole('staff', 'manager', 'admin')") // RBAC
    fun shareFile(
        @Parameter(description = "UUID of the file to share", required = true)
        @PathVariable id: UUID,
        @RequestBody req: CreateShareRequest
    ): ResponseEntity<ShareDTO> {
        logger.info("REST: Request to share file {} with user {}", id, req.userId)

        val share = shareService.shareFile(id, req.userId, req.permission)

        val responseDTO = ShareDTO(share.id!!, share.file!!.id!!, share.sharedWithUserId!!, share.permission)
        return ResponseEntity.ok(responseDTO)
    }

    /**
     * Endpoint: GET /api/v1/files/{id}/shares
     */
    @Operation(summary = "Get File Shares", description = "Retrieves a list of all users who have access to this file.")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "List of shares retrieved"),
        ApiResponse(responseCode = "404", description = "File not found")
    ])
    @GetMapping("/{id}/shares")
    @PreAuthorize("hasAnyRole('staff', 'manager', 'admin')")
    fun getShares(
        @Parameter(description = "UUID of the file", required = true)
        @PathVariable id: UUID): ResponseEntity<List<ShareDTO>> {
        logger.info("REST: Get shares for file {}", id)
        // Service needs ACL check (Owner only)
        val shares = shareService.getSharesForFile(id)
        val dtos = shares.map { ShareDTO(it.id!!, it.file!!.id!!, it.sharedWithUserId!!, it.permission) }
        return ResponseEntity.ok(dtos)
    }

    /**
     * Endpoint: GET /api/v1/files/shared-with/{userId}
     */
    @Operation(summary = "Get Shared Files for User", description = "List all files shared with a specific target user.")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "List of shared files retrieved")
    ])
    @GetMapping("/shared-with/{userId}")
    @PreAuthorize("hasAnyRole('staff', 'manager', 'admin')")
    fun getFilesSharedWith(
        @Parameter(description = "UUID of the target user", required = true)
        @PathVariable userId: UUID): ResponseEntity<List<ShareDTO>> {
        logger.info("REST: Get files shared with user {}", userId)
        val shares = shareService.getFilesSharedWithUser(userId)
        val dtos = shares.map { ShareDTO(it.id!!, it.file!!.id!!, it.sharedWithUserId!!, it.permission) }
        return ResponseEntity.ok(dtos)
    }

    /**
     * Endpoint: DELETE /api/v1/files/{id}/shares/{shareId}
     */
    @Operation(summary = "Revoke Share", description = "Removes a user's access to a file.")
    @ApiResponses(value = [
        ApiResponse(responseCode = "204", description = "Share revoked successfully"),
        ApiResponse(responseCode = "404", description = "Share not found or does not belong to file"),
        ApiResponse(responseCode = "400", description = "Invalid UUIDs")
    ])
    @DeleteMapping("/{id}/shares/{shareId}")
    @PreAuthorize("hasAnyRole('staff', 'manager', 'admin')")
    fun deleteShare(
        @Parameter(description = "UUID of the file", required = true)
        @PathVariable id: UUID,
        @Parameter(description = "UUID of the share record to remove", required = true)
        @PathVariable shareId: UUID
    ): ResponseEntity<Void> {
        logger.info("REST: Revoke share {} for file {}", shareId, id)
        // Service needs ACL Check (Owner Only)
        shareService.removeShare(id, shareId)
        return ResponseEntity.noContent().build()
    }

    /**
     * Endpoint: POST /api/v1/files/{id}/versions
     * Requirement: Start a TUS upload session for a new version.
     */
    @Operation(summary = "Create New Version Upload", description = "Starts a TUS upload session to add a new version to an existing file.")
    @ApiResponses(value = [
        ApiResponse(responseCode = "201", description = "Upload session created", headers = [
            Header(name = "Location", description = "TUS Upload URL", schema = Schema(type = "string")),
            Header(name = "Tus-Resumable", description = "Protocol version", schema = Schema(type = "string"))
        ]),
        ApiResponse(responseCode = "404", description = "Original file not found"),
        ApiResponse(responseCode = "400", description = "Invalid headers")
    ])
    @PostMapping("/{id}/versions")
    @PreAuthorize("hasAnyRole('staff', 'manager', 'admin')")
    fun createVersionUpload(
        @Parameter(description = "UUID of the existing file", required = true)
        @PathVariable id: UUID,
        @Parameter(description = "Size of the new version in bytes", required = true)
        @RequestHeader("Upload-Length") uploadLength: Long,
        @Parameter(description = "Metadata encoded in Base64", required = false)
        @RequestHeader("Upload-Metadata", required = false) metadata: String?,
        @Parameter(description = "Dev: Owner injection", required = false)
        @RequestHeader("X-User-Id", required = false) userIdHeader: String?
    ): ResponseEntity<Void> {
        logger.info("REST: Request to create NEW VERSION for file {}", id)
        // Note: userIdHeader is ignored in service in favor of SecurityContext
        val ownerId = if (userIdHeader != null) UUID.fromString(userIdHeader) else UUID.randomUUID()

        // Service checks ACL (Write Permission)
        val session = uploadService.createUploadForVersion(id, uploadLength, metadata, ownerId)

        val locationUrl = "http://localhost:8080/uploads/${session.id}"
        return ResponseEntity.status(HttpStatus.CREATED)
            .header("Location", locationUrl)
            .header("Tus-Resumable", "1.0.0")
            .build()
    }

    /**
     * Endpoint: GET /api/v1/files/{id}/preview
     */
    @Operation(summary = "Get File Preview", description = "Downloads a generated preview (image/jpeg) if available.")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Preview image retrieved", content = [Content(mediaType = "image/jpeg")]),
        ApiResponse(responseCode = "404", description = "Preview not found or file not found"),
        ApiResponse(responseCode = "400", description = "Invalid UUID format")
    ])
    @GetMapping("/{id}/preview")
    @PreAuthorize("hasAnyRole('staff', 'manager', 'admin')")
    fun getFilePreview(
        @Parameter(description = "UUID of the file", required = true)
        @PathVariable id: String): ResponseEntity<Resource> {
        val fileId = try { UUID.fromString(id) } catch (_: IllegalArgumentException) { throw IllegalArgumentException("Invalid UUID format: $id") }
        logger.info("REST: Requesting preview for file {}", id)

        val stream = uploadService.getPreviewStream(fileId)

        if (stream == null) {
            logger.warn("Preview not found for file {}", id)
            return ResponseEntity.notFound().build()
        }

        val resource = InputStreamResource(stream)
        return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).body(resource)
    }

    /**
     * Endpoint: GET /api/v1/files/{id}/history
     */
    @Operation(summary = "Get File History", description = "Retrieves the list of all versions for a file.")
    @GetMapping("/{id}/history")
    @PreAuthorize("hasAnyRole('staff', 'manager', 'admin')")
    fun getFileHistory(@PathVariable id: UUID): ResponseEntity<List<FileVersionDTO>> {
        logger.info("REST: Request history for file {}", id)
        // Service checks ACL (Read Permission)
        val history = uploadService.getFileHistory(id)
        return ResponseEntity.ok(history)
    }

    /**
     * Endpoint: GET /api/v1/files/{id}/versions/{version}/download
     * Requirement: Download a specific old version.
     */
    @Operation(summary = "Download Specific Version", description = "Downloads the binary content of a specific file version.")
    @GetMapping("/{id}/versions/{version}/download")
    @PreAuthorize("hasAnyRole('staff', 'manager', 'admin')")
    fun downloadVersion(
        @PathVariable id: UUID,
        @PathVariable version: Int
    ): ResponseEntity<InputStreamResource> {
        logger.info("REST: Download file {} version {}", id, version)
        // Service checks ACL (Read Permission)
        val (fileVersionEntry, inputStream) = uploadService.getSpecificVersionStream(id, version)
        val resource = InputStreamResource(inputStream)

        val headers = HttpHeaders()
        headers.contentType = MediaType.parseMediaType(fileVersionEntry.contentType)
        headers.contentLength = fileVersionEntry.size
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"${fileVersionEntry.filename}\"")
        return ResponseEntity.ok().headers(headers).body(resource)
    }

    /**
     * Endpoint: DELETE /api/v1/files/{id}/versions/latest
     * Feature: Rollback to previous version.
     * Removes the latest version, effectively reverting the file state.
     */
    @Operation(summary = "Rollback Version", description = "Deletes the latest version of the file, reverting to the previous one. Requires at least 2 versions.")
    @ApiResponses(value = [
        ApiResponse(responseCode = "204", description = "Rollback successful"),
        ApiResponse(responseCode = "400", description = "Cannot rollback (only 1 version exists)"),
        ApiResponse(responseCode = "404", description = "File not found")
    ])
    @DeleteMapping("/{id}/versions/latest")
    @PreAuthorize("hasAnyRole('staff', 'manager', 'admin')")
    fun rollbackVersion(
        @Parameter(description = "UUID of the file to rollback")
        @PathVariable id: UUID): ResponseEntity<Void> {
        logger.info("REST: Request to rollback file {}", id)
        // Service checks ACL (Write Permission)
        uploadService.rollbackToPreviousVersion(id)

        return ResponseEntity.noContent().build()
    }

    @PostMapping("/{fileId}/versions/{versionId}/ai-metadata")
    @PreAuthorize("hasAnyRole('staff', 'manager', 'admin', 'ROLE_AI_WORKER')")
    fun updateAiMetadata(
        @PathVariable fileId: UUID,
        @PathVariable versionId: Int,
        @RequestBody aiData: AiResultDTO
    ): ResponseEntity<Void> {

        logger.info("[FILE-SERVICE] AI update received for file $fileId v$versionId.")
        logger.info("DATA: Summary='${aiData.summary}', Tags=${aiData.tags}, Sens='${aiData.sensitivity}'")
        // -------------------------------------

        uploadService.saveAiMetadata(fileId, versionId, aiData)

        return ResponseEntity.ok().build()
    }

    @PostMapping("/{fileId}/versions/{versionId}/ai-failure")
    @PreAuthorize("hasAnyRole('staff', 'manager', 'admin', 'ROLE_AI_WORKER')")
    fun setAiStatusFailed(
        @PathVariable fileId: UUID,
        @PathVariable versionId: Int
    ): ResponseEntity<Void> {

        logger.info("[FILE-SERVICE] Updating AI status to FAILURE for file $fileId v$versionId.")
        // -------------------------------------

        uploadService.setAiMetadataFailure(fileId, versionId)

        return ResponseEntity.ok().build()
    }

    @PostMapping("/query")
    @PreAuthorize("hasAnyRole('staff', 'manager', 'admin')")
    fun askTheLLM(
        @RequestBody request: QueryQuestionDTO,

    ): ResponseEntity<String> {
        logger.info("REST: Request to ask theLLM for question {}", request.question)

        val currentUser = securityUtils.getCurrentUser()

        // CHIAMATA AI: Passa la domanda + la lista degli ID permessi
        val answer = uploadService.askQuestionToAi(
            userId = currentUser.id,
            question = request.question,
            specificFileIds = request.specificFileIds
        )

        return ResponseEntity.ok(answer)
    }


    /**
     * Endpoint: POST /api/v1/files/{id}/ai-retry
     * Feature: Manual intervention on AI processing (Req. 6.e)
     */
    @Operation(summary = "Retry AI Analysis", description = "Manually re-triggers the AI processing pipeline for the latest version of the file.")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Retry event dispatched successfully"),
        ApiResponse(responseCode = "404", description = "File not found"),
        ApiResponse(responseCode = "403", description = "Insufficient permissions")
    ])
    @PostMapping("/{id}/ai-retry")
    @PreAuthorize("hasAnyRole('staff', 'manager', 'admin')")
    fun retryAiAnalysis(
        @Parameter(description = "UUID of the file to re-analyze", required = true)
        @PathVariable id: String
    ): ResponseEntity<Void> {
        val fileId = try {
            UUID.fromString(id)
        } catch (_: IllegalArgumentException) {
            throw IllegalArgumentException("Invalid UUID format: $id")
        }

        logger.info("REST: Request to manually retry AI analysis for file {}", fileId)
        uploadService.triggerAiRetry(fileId)

        return ResponseEntity.ok().build()
    }
}