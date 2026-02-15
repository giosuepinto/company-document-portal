package it.polito.wa2_2026.g15.fileservice.services

// IMPORTS: DTOs
import com.fasterxml.jackson.databind.ObjectMapper
import it.polito.wa2_2026.g15.fileservice.dto.AiResultDTO
import it.polito.wa2_2026.g15.fileservice.dto.FileMetadataDTO
import it.polito.wa2_2026.g15.fileservice.dto.FileUpdateDTO
import it.polito.wa2_2026.g15.fileservice.dto.FileVersionDTO

// IMPORTS: ENTITIES
import it.polito.wa2_2026.g15.fileservice.entities.File
import it.polito.wa2_2026.g15.fileservice.entities.FileVersion
import it.polito.wa2_2026.g15.fileservice.entities.FileVersionId
import it.polito.wa2_2026.g15.fileservice.entities.Upload
import it.polito.wa2_2026.g15.fileservice.entities.UploadStatus
import it.polito.wa2_2026.g15.fileservice.entities.SharePermission

// IMPORTS: EXCEPTIONS
import it.polito.wa2_2026.g15.fileservice.exceptions.ChecksumMismatchException
import it.polito.wa2_2026.g15.fileservice.exceptions.FileNotFoundException
import it.polito.wa2_2026.g15.fileservice.exceptions.InvalidContentTypeException
import it.polito.wa2_2026.g15.fileservice.exceptions.InvalidTusVersionException
import it.polito.wa2_2026.g15.fileservice.exceptions.InvalidUploadOffsetException
import it.polito.wa2_2026.g15.fileservice.exceptions.PayloadTooLargeException
import it.polito.wa2_2026.g15.fileservice.exceptions.PermissionDeniedException
import it.polito.wa2_2026.g15.fileservice.exceptions.StorageException
import it.polito.wa2_2026.g15.fileservice.exceptions.UploadAlreadyCompletedException
import it.polito.wa2_2026.g15.fileservice.exceptions.UploadLengthException
import it.polito.wa2_2026.g15.fileservice.exceptions.UploadNotFoundException

// IMPORTS: REPOSITORIES
import it.polito.wa2_2026.g15.fileservice.repositories.FileRepository
import it.polito.wa2_2026.g15.fileservice.repositories.FileVersionRepository
import it.polito.wa2_2026.g15.fileservice.repositories.UploadRepository

// IMPORTS: SECURITY
import it.polito.wa2_2026.g15.fileservice.utils.SecurityUtils
import org.springframework.security.access.AccessDeniedException

//IMPORTS: OUTBOX (previously KAFKA)
import it.polito.wa2_2026.g15.fileservice.dto.FileUploadEventDTO
import it.polito.wa2_2026.g15.fileservice.entities.AiStatus
import it.polito.wa2_2026.g15.fileservice.entities.Outbox
import it.polito.wa2_2026.g15.fileservice.repositories.OutboxRepository

// IMPORTS: STANDARD JAVA/SPRING
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.*
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.security.MessageDigest
import java.time.Instant
import java.util.*


@Service
@Transactional
class UploadServiceImpl(
    private val uploadRepository: UploadRepository,
    private val storageService: StorageService,
    private val fileRepository: FileRepository,
    private val fileVersionRepository: FileVersionRepository,
    private val previewService: PreviewService,
    private val securityUtils: SecurityUtils,
    @Value("\${minio.bucket-name}") private val bucketName: String,
    private val userClient: UserClient,
    private val aiClient: AiProcessingClient,
    private val outboxRepository: OutboxRepository,
    private val objectMapper: ObjectMapper
) : UploadService {

    private val logger = LoggerFactory.getLogger(UploadServiceImpl::class.java)
    private val BUFFER_DIR: Path = Paths.get(System.getProperty("java.io.tmpdir"), "tus-buffers")
    private val MIN_PART_SIZE: Long = 5 * 1024 * 1024

    init {
        try { Files.createDirectories(BUFFER_DIR) }
        catch (e: IOException) { logger.error("Failed to create temp buffer directory", e) }
    }

    // --- ACL CHECKS (ROBUST) ---

    private fun checkReadPermission(file: File) {
        val user = securityUtils.getCurrentUser()
        if (user.isAdmin()) return
        if (file.owner == user.id) return

        // Share Check
        if (file.sharingRules.any { it.sharedWithUserId == user.id }) return

        // Department Check (Manager & Staff)
        if (user.isManager() || user.isStaff()) {
            val userDetails = userClient.getUserDetails(user.id)
            val fileOwnerDetails = file.owner?.let { userClient.getUserDetails(it) }

            if (userDetails?.department != null && userDetails.department == fileOwnerDetails?.department) {
                return
            }
        }
        throw PermissionDeniedException("Read Access Denied for user ${user.id}")
    }

    private fun checkWritePermission(file: File) {
        val user = securityUtils.getCurrentUser()

        // 1. Admin & Owner
        if (user.isAdmin()) return
        if (file.owner == user.id) return

        // 2. Explicit WRITE Share
        val hasWriteShare = file.sharingRules.any {
            it.sharedWithUserId == user.id && it.permission == SharePermission.WRITE
        }
        if (hasWriteShare) return

        // 3. Manager Department Access
        if (user.isManager()) {
            val userDetails = userClient.getUserDetails(user.id)
            val fileOwnerDetails = file.owner?.let { userClient.getUserDetails(it) }

            logger.info("ACL Manager Check: UserDept=${userDetails?.department}, FileDept=${fileOwnerDetails?.department}")

            if (userDetails?.department != null &&
                fileOwnerDetails?.department != null &&
                userDetails.department == fileOwnerDetails.department) {
                return
            }
        }

        logger.warn("Write Access Denied. User: ${user.id}, Role: ${user.roles}, ShareWrite: $hasWriteShare")
        throw PermissionDeniedException("Write Access Denied for user ${user.id}")
    }

    // --- TUS HELPERS ---
    private fun parseMetadata(base64Metadata: String?): Map<String, String> {
        if (base64Metadata.isNullOrBlank()) return emptyMap()
        return try {
            base64Metadata.split(",").associate { pair ->
                val parts = pair.trim().split(" ", limit = 2)
                if (parts.size == 2) parts[0] to String(Base64.getDecoder().decode(parts[1]), Charsets.UTF_8) else parts[0] to ""
            }
        } catch (_: Exception) { emptyMap() }
    }

    override fun verifyTusHeader(tusResumable: String) {
        if (tusResumable != "1.0.0") throw InvalidTusVersionException("Tus version must be 1.0.0")
    }
    override fun verifyTusPostHeader(tusResumable: String, uploadLength: Long, maxLength: Long) {
        verifyTusHeader(tusResumable)
        if (uploadLength == 0L) throw UploadLengthException("Zero length")
        if (uploadLength > maxLength) throw PayloadTooLargeException("Payload too large")
    }
    override fun verifyTusPatchHeader(tusResumable: String, contentType: String) {
        verifyTusHeader(tusResumable)
        if (!contentType.startsWith("application/offset+octet-stream")) throw InvalidContentTypeException("Invalid Content-Type")
    }
    private fun calculateSha256Base64(data: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return Base64.getEncoder().encodeToString(digest.digest(data))
    }

    // --- CREATION ---

    override fun createUpload(uploadLength: Long, metadata: String?, ownerId: UUID): Upload {
        val currentUser = securityUtils.getCurrentUser()

        // Owner della sessione TUS è SEMPRE chi fa l'upload
        val sessionOwnerId = currentUser.id

        val metadataMap = parseMetadata(metadata)
        val filename = metadataMap["filename"] ?: "upload_${UUID.randomUUID()}"
        val contentType = metadataMap["contentType"] ?: "application/octet-stream"

        val newUpload = Upload().apply {
            this.size = uploadLength
            this.status = UploadStatus.PENDING
            this.createdAt = Instant.now()
            this.totalOffset = 0L
            this.chunkCount = 0
            this.filename = filename
            this.contentType = contentType
            this.jsonMetadata = metadata ?: ""
            this.bucket = bucketName
            this.relatedFileId = null
            this.ownerId = sessionOwnerId // <--- FIX
        }

        val saved = uploadRepository.save(newUpload)
        saved.objectKey = "uploads/${saved.id}/$filename"
        return uploadRepository.save(saved)
    }

    override fun createUploadForVersion(fileId: UUID, uploadLength: Long, metadata: String?, ownerId: UUID): Upload {
        val file = fileRepository.findById(fileId).orElseThrow { FileNotFoundException(fileId) }

        // 1. Verifico se ho il diritto di scrivere sul file originale
        checkWritePermission(file)

        val currentUser = securityUtils.getCurrentUser()
        // 2. La sessione temporanea è MIA (Staff), anche se il file è del Manager
        val sessionOwnerId = currentUser.id

        val metadataMap = parseMetadata(metadata)
        val newUpload = Upload().apply {
            this.size = uploadLength
            this.status = UploadStatus.PENDING
            this.createdAt = Instant.now()
            this.totalOffset = 0L
            this.chunkCount = 0
            this.filename = metadataMap["filename"] ?: "temp_version"
            this.contentType = metadataMap["contentType"] ?: "application/octet-stream"
            this.jsonMetadata = metadata ?: ""
            this.bucket = bucketName
            this.relatedFileId = fileId
            this.ownerId = sessionOwnerId // <--- FIX IMPORTANTE

        }

        val saved = uploadRepository.save(newUpload)
        saved.objectKey = "uploads/${saved.id}/${saved.filename}"
        return uploadRepository.save(saved)
    }

    // --- UPLOAD PROCESS ---

    @Transactional(readOnly = true)
    override fun getUploadStatus(id: UUID): Upload {
        val upload = uploadRepository.findById(id).orElseThrow { UploadNotFoundException(id.toString()) }
        val currentUser = securityUtils.getCurrentUser()

        // Controllo solo la sessione temporanea
        if (!currentUser.isAdmin() && upload.ownerId != currentUser.id) {
            throw PermissionDeniedException("User ${currentUser.id} cannot access this upload session")
        }
        return upload
    }

    @Transactional // Assicura che se l'outbox fallisce, il file non venga considerato "finalizzato"
    override fun writeChunk(id: UUID, uploadOffset: Long, inputStream: InputStream, clientChecksumHeader: String?): Upload {
        val upload = uploadRepository.findById(id).orElseThrow { UploadNotFoundException(id.toString()) }
        val currentUser = securityUtils.getCurrentUser()

        // Controllo cruciale: Chi sta scrivendo il chunk deve essere chi ha iniziato l'upload
        if (!currentUser.isAdmin() && upload.ownerId != currentUser.id) {
            throw PermissionDeniedException("User ${currentUser.id} cannot modify this upload session")
        }

        if (upload.status == UploadStatus.COMPLETED) throw UploadAlreadyCompletedException("Completed")
        if (uploadOffset != upload.totalOffset) throw InvalidUploadOffsetException("Offset mismatch")

        val chunkData: ByteArray
        try {
            val buffer = ByteArrayOutputStream()
            inputStream.copyTo(buffer)
            chunkData = buffer.toByteArray()
        } catch (_: IOException) { throw StorageException("Read failed") }

        if (chunkData.isEmpty()) return upload

        if (clientChecksumHeader != null) {
            val parts = clientChecksumHeader.split(" ")
            if (parts.size == 2 && parts[0].equals("sha256", ignoreCase = true)) {
                if (parts[1] != calculateSha256Base64(chunkData)) throw ChecksumMismatchException("Checksum mismatch")
            }
        }

        val bufferFile = BUFFER_DIR.resolve("${id}.part")
        try { Files.write(bufferFile, chunkData, StandardOpenOption.CREATE, StandardOpenOption.APPEND) }
        catch (_: IOException) { throw StorageException("Write buffer failed") }

        upload.totalOffset += chunkData.size
        upload.status = UploadStatus.UPLOADING

        val currentBufferSize = Files.size(bufferFile)
        if (upload.totalOffset == upload.size) {
            if (currentBufferSize > 0) {
                uploadPartToMinIO(upload, bufferFile, currentBufferSize)
                upload.chunkCount++
                Files.deleteIfExists(bufferFile)
            }
            composeFinalObject(upload)
            finalizeUpload(upload)
            return upload
        } else if (currentBufferSize >= MIN_PART_SIZE) {
            uploadPartToMinIO(upload, bufferFile, currentBufferSize)
            upload.chunkCount++
            Files.deleteIfExists(bufferFile)
        }
        return uploadRepository.save(upload)
    }

    private fun uploadPartToMinIO(upload: Upload, bufferFile: Path, size: Long) {
        val partKey = "uploads/${upload.id}/parts/${upload.chunkCount}"
        try {
            Files.newInputStream(bufferFile).use {
                storageService.putObject(bucketName, partKey, it, size, "application/octet-stream")
            }
        } catch (_: Exception) { throw StorageException("Upload part failed") }
    }

    private fun composeFinalObject(upload: Upload) {
        try {
            val parts = (0 until upload.chunkCount).map { "uploads/${upload.id}/parts/$it" }
            if (parts.isEmpty() && upload.size == 0L) {
                storageService.putObject(bucketName, upload.objectKey, ByteArrayInputStream(ByteArray(0)), 0, upload.contentType)
            } else {
                val etag = storageService.composeObject(bucketName, upload.objectKey, parts)
                upload.checksum = etag
                parts.forEach { try { storageService.deleteObject(bucketName, it) } catch (_: Exception) {} }
            }
            upload.status = UploadStatus.COMPLETED
            upload.completedAt = Instant.now()
            logger.info("Final object composed.")
        } catch (_: Exception) {
            upload.status = UploadStatus.FAILED
            throw StorageException("Compose failed")
        }
    }

    private fun finalizeUpload(upload: Upload) {
        try {
            val targetFile: File
            val versionNum: Int

            if (upload.relatedFileId == null) {
                // Nuovo File: L'owner diventa chi ha caricato
                targetFile = File().apply {
                    id = UUID.randomUUID()
                    owner = upload.ownerId
                    createdAt = Instant.now()
                    updatedAt = Instant.now()
                }
                fileRepository.save(targetFile)
                versionNum = 1
            } else {
                // Nuova Versione: L'owner del file resta quello originale (es. Manager)
                targetFile = fileRepository.findById(upload.relatedFileId!!).orElseThrow { FileNotFoundException(upload.relatedFileId!!) }

                // Ricontrollo i permessi sul file originale per sicurezza (es. share write ancora valida?)
                checkWritePermission(targetFile)

                val lastVersion = fileVersionRepository.findTopByFileOrderByUpdatedAtDesc(targetFile)
                versionNum = (lastVersion?.id?.version ?: 0) + 1
                targetFile.updatedAt = Instant.now()
                fileRepository.save(targetFile)
            }

            val finalKey = "files/${targetFile.id}/v$versionNum/${upload.filename}"
            var previewKey: String? = null
            var previewBytes: ByteArray? = null

            try {
                val src = storageService.getObject(bucketName, upload.objectKey)
                previewBytes = previewService.generatePreview(src, upload.contentType)
                if(previewBytes != null) previewKey = "files/${targetFile.id}/v$versionNum/preview.jpg"
            } catch(e: Exception) { logger.warn("Preview gen failed", e) }

            val ver = FileVersion().apply {
                id = FileVersionId(targetFile.id, versionNum)
                file = targetFile
                size = upload.size
                checksum = upload.checksum
                contentType = upload.contentType
                filename = upload.filename
                updatedAt = Instant.now()
                minioObjectKey = finalKey
                previewMinioKey = previewKey
            }
            fileVersionRepository.save(ver)

            if(previewBytes != null && previewKey != null) {
                try { storageService.putObject(bucketName, previewKey, ByteArrayInputStream(previewBytes), previewBytes.size.toLong(), "image/jpeg") }
                catch(_:Exception){}
            }

            // --- INIZIO BLOCCO OUTBOX (Sostituisce il blocco Kafka) ---
            if (ver.contentType.startsWith("text/") || ver.contentType == "application/pdf") {
                try {
                    // Prepariamo l'oggetto evento (lo stesso che inviavamo a Kafka)
                    val event = FileUploadEventDTO(
                        fileId = targetFile.id!!,
                        fileVersionId = versionNum,
                        ownerId = targetFile.owner!!,
                        filename = upload.filename,
                        contentType = upload.contentType,
                        size = upload.size
                    )

                    // Serializziamo l'evento in JSON (usando ObjectMapper di Jackson)
                    val jsonPayload = objectMapper.writeValueAsString(event)

                    // CREAZIONE RECORD NELLA TABELLA OUTBOX
                    // Questo inserimento è parte della STESSA transazione DB del salvataggio file
                    val outboxRecord = Outbox(
                        aggregateid = targetFile.id.toString(),
                        aggregatetype = "file-upload.completed",
                        payload = jsonPayload
                    )

                    outboxRepository.save(outboxRecord)
                    logger.info("Outbox record created for file ${targetFile.id}")

                } catch (ex: Exception) {
                    logger.error("Failed to write to Outbox table", ex)
                    throw RuntimeException("Persistence failed: Outbox could not be written")
                }
            }
            // --- FINE BLOCCO OUTBOX ---

            // Spostamento fisico su MinIO e pulizia sessione TUS
            logger.info("Bucket name: {}, objectKey: {}, finalKey: {}", bucketName, upload.objectKey, finalKey)
            storageService.moveObject(bucketName, upload.objectKey, finalKey)
            uploadRepository.removeById(upload.id!!)
        } catch (e: Exception) {
            upload.status = UploadStatus.FAILED
            throw e
        }
    }

    @Transactional
    override fun terminateUpload(id: UUID) {
        val upload = uploadRepository.findByIdOrNull(id)
        if (upload != null) {
            val user = securityUtils.getCurrentUser()
            if (!user.isAdmin() && upload.ownerId != user.id) throw PermissionDeniedException("Denied")
            try { storageService.deleteObject(bucketName, upload.objectKey) } catch (_: Exception) {}
            uploadRepository.delete(upload)
            return
        }
        val file = fileRepository.findById(id).orElseThrow { FileNotFoundException(id) }
        val user = securityUtils.getCurrentUser()
        if (!user.isAdmin() && file.owner != user.id) throw PermissionDeniedException("Denied")
        file.versions.forEach {
            try { if (it.minioObjectKey.isNotEmpty()) storageService.deleteObject(bucketName, it.minioObjectKey) } catch (_: Exception) {}
        }
        fileRepository.delete(file)
    }

    @Transactional(readOnly = true)
    override fun getDownloadStream(id: UUID): Pair<FileVersion, InputStream> {
        val file = fileRepository.findById(id).orElseThrow { FileNotFoundException(id) }
        checkReadPermission(file)
        val ver = fileVersionRepository.findTopByIdFileIdOrderByUpdatedAtDesc(id) ?: throw FileNotFoundException(id)
        return Pair(ver, storageService.getObject(bucketName, ver.minioObjectKey))
    }

    @Transactional(readOnly = true)
    override fun getPersistentFiles(owner: UUID?, pageable: Pageable): Page<File> {
        val user = securityUtils.getCurrentUser()
        if (owner != null) {
            if (!user.isAdmin() && owner != user.id) throw PermissionDeniedException("Denied")
            return fileRepository.findByOwner(owner, pageable)
        }
        if (user.isAdmin()) return fileRepository.findAll(pageable)

        val visibleOwners = mutableListOf(user.id)
        val details = userClient.getUserDetails(user.id)
        if (!(details == null || details.department.isNullOrBlank())) {
            visibleOwners.addAll(userClient.getUsersInDepartment(details.department))
        }
        return fileRepository.findAllVisibleFiles(user.id, visibleOwners, pageable)
    }

    @Transactional(readOnly = true)
    override fun getPersistentFile(id: UUID): File {
        val file = fileRepository.findById(id).orElseThrow { FileNotFoundException(id) }
        checkReadPermission(file)
        return file
    }

    override fun updatePersistentFileMetadata(id: UUID, updateDTO: FileUpdateDTO): FileMetadataDTO {
        val file = fileRepository.findById(id).orElseThrow { FileNotFoundException(id) }
        checkWritePermission(file)

        val latest = fileVersionRepository.findTopByFileOrderByUpdatedAtDesc(file) ?: throw IllegalStateException("No versions")
        var changed = false
        updateDTO.filename?.let { if (it.isNotBlank() && it != latest.filename) { latest.filename = it; changed = true } }
        updateDTO.contentType?.let { if (it.isNotBlank() && it != latest.contentType) { latest.contentType = it; changed = true } }

        if (changed) {
            val now = Instant.now()
            latest.updatedAt = now
            file.updatedAt = now
            fileRepository.save(file)
            fileVersionRepository.save(latest)
        }
        // Il controller ricalcolerà il permesso corretto
        return FileMetadataDTO(file.id!!, latest.filename, latest.contentType, latest.size, UploadStatus.COMPLETED, file.createdAt, file.owner.toString(), file.versions.size, "WRITE", latest.summary, latest.tags, latest.sensitivity)
    }

    // ... getPreviewStream, getFileHistory, getSpecificVersionStream, rollbackToPreviousVersion (INVARIATI) ...
    // Assicurati che checkReadPermission/checkWritePermission siano chiamati come nel codice precedente.
    // Per brevità non li ripeto se non sono cambiati, ma nel file finale devono esserci.

    // Rimetto i metodi mancanti per completezza copia-incolla
    override fun getPreviewStream(fileId: UUID): InputStream? {
        val file = fileRepository.findById(fileId).orElse(null) ?: return null
        try { checkReadPermission(file) } catch (_: AccessDeniedException) { return null }
        val ver = fileVersionRepository.findTopByIdFileIdOrderByUpdatedAtDesc(fileId) ?: return null
        return ver.previewMinioKey?.let { try { storageService.getObject(bucketName, it) } catch (_: Exception) { null } }
    }

    @Transactional(readOnly = true)
    override fun getFileHistory(fileId: UUID): List<FileVersionDTO> {
        val file = fileRepository.findById(fileId).orElseThrow { FileNotFoundException(fileId) }
        checkReadPermission(file)
        return fileVersionRepository.findAllByIdFileIdOrderByIdVersionDesc(fileId).map {
            FileVersionDTO(it.id!!.version, it.filename, it.contentType, it.size, it.updatedAt)
        }
    }

    @Transactional(readOnly = true)
    override fun getSpecificVersionStream(fileId: UUID, version: Int): Pair<FileVersion, InputStream> {
        val file = fileRepository.findById(fileId).orElseThrow { FileNotFoundException(fileId) }
        checkReadPermission(file)
        val ver = fileVersionRepository.findByIdFileIdAndIdVersion(fileId, version) ?: throw FileNotFoundException(fileId)
        return Pair(ver, storageService.getObject(bucketName, ver.minioObjectKey))
    }

    @Transactional
    override fun rollbackToPreviousVersion(fileId: UUID) {
        val file = fileRepository.findById(fileId).orElseThrow { FileNotFoundException(fileId) }
        checkWritePermission(file)
        val versions = file.versions.sortedByDescending { it.id?.version }
        if (versions.size <= 1) throw IllegalArgumentException("Cannot rollback")
        val latest = versions[0]
        try {
            if (latest.minioObjectKey.isNotEmpty()) storageService.deleteObject(bucketName, latest.minioObjectKey)
            latest.previewMinioKey?.let { storageService.deleteObject(bucketName, it) }
        } catch (_: Exception) {}
        file.versions.remove(latest)
        file.updatedAt = Instant.now()
        fileVersionRepository.delete(latest)
        fileRepository.save(file)
    }

    // Nel Service (assicurati di averlo aggiunto anche all'interfaccia se presente)
    @Transactional
    override fun saveAiMetadata(fileId: UUID, versionId: Int, aiData: AiResultDTO) {
        // 1. Crea l'ID composto
        val id = FileVersionId(fileId, versionId)

        // 2. Cerca la versione (se non c'è, lancia eccezione)
        val fileVersion = fileVersionRepository.findById(id)
            .orElseThrow { RuntimeException("File version not found") }

        // 3. Aggiorna i campi
        fileVersion.summary = aiData.summary
        fileVersion.sensitivity = aiData.sensitivity

        // Pulisci e aggiorna i tags
        fileVersion.tags.clear()
        fileVersion.tags.addAll(aiData.tags)
        fileVersion.ai_status = AiStatus.COMPLETED

        // 4. Salva (JPA gestirà l'update)
        fileVersionRepository.save(fileVersion)
    }

    @Transactional
    override fun setAiMetadataFailure(fileId: UUID, versionId: Int) {
        // Crea l'ID composto
        val id = FileVersionId(fileId, versionId)

        // Cerca la versione (se non c'è, lancia eccezione)
        val fileVersion = fileVersionRepository.findById(id)
            .orElseThrow { RuntimeException("File version not found") }

        fileVersion.ai_status = AiStatus.FAILED

        // Salva (JPA gestirà l'update)
        fileVersionRepository.save(fileVersion)
    }

    override fun askQuestionToAi(userId: UUID, question: String, specificFileIds: List<String>?): String {
        // 1. SECURITY: Calcola la whitelist degli ID permessi (logica che abbiamo già scritto)
        val allowedFileIds = getAllAllowedFileIds(userId)

        if (allowedFileIds.isEmpty()) {
            return "You have no access to any document."
        }

        // 2. FILTRAGGIO E VALIDAZIONE
        val finalTargetIds = if (!specificFileIds.isNullOrEmpty()) {
            val allowedStrings = allowedFileIds.map { it.toString() }
            allowedStrings.intersect(specificFileIds.toSet()).toList()
        } else {
            allowedFileIds.map { it.toString() }
        }

        if (finalTargetIds.isEmpty()) {
            return "No listed file exists or is accessible."
        }

        // 3. CHIAMATA AL CLIENT (Integrazione esterna)
        return aiClient.queryRAG(question, finalTargetIds)
    }

    @Transactional(readOnly = true)
    override fun getAllAllowedFileIds(userId: UUID): List<UUID> {
        val user = securityUtils.getCurrentUser()

        // 1. ADMIN
        if (user.isAdmin()) {
            return fileRepository.findAllIds() // Definiamo questa query dopo
        }

        // 2. MANAGER o STAFF
        val visibleOwners = mutableListOf(userId)

        if (user.isManager()) {
            val details = userClient.getUserDetails(userId)
            details?.department?.takeIf { it.isNotBlank() }?.let { dept ->
                val departmentMembers = userClient.getUsersInDepartment(dept)
                visibleOwners.addAll(departmentMembers)
            }
        }

        // 3. Executes optimized query
        // looks for files which:
        // - are owned by me OR by my department (if I am Manager)
        // - OR expicitilly shared with m
        return fileRepository.findAllAllowedIds(userId, visibleOwners)
    }
}