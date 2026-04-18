package it.polito.wa2_2026.g15.fileservice.services

// importing the entity
import it.polito.wa2_2026.g15.fileservice.dto.AiResultDTO
import it.polito.wa2_2026.g15.fileservice.dto.FileMetadataDTO
import it.polito.wa2_2026.g15.fileservice.dto.FileUpdateDTO
import it.polito.wa2_2026.g15.fileservice.dto.FileVersionDTO
import it.polito.wa2_2026.g15.fileservice.entities.File
import it.polito.wa2_2026.g15.fileservice.entities.FileVersion
import it.polito.wa2_2026.g15.fileservice.entities.Upload
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.io.InputStream
import java.util.UUID

interface UploadService {

    fun createUpload(uploadLength: Long, metadata: String?, ownerId: UUID): Upload

    fun createUploadForVersion(fileId: UUID, uploadLength: Long, metadata: String?, ownerId: UUID): Upload

    fun getUploadStatus(id: UUID): Upload

    fun writeChunk(id: UUID, uploadOffset: Long, inputStream: InputStream, clientChecksumHeader: String?): Upload

    fun terminateUpload(id: UUID)

    fun getDownloadStream(id: UUID): Pair<FileVersion, InputStream>

    fun verifyTusHeader(tusResumable : String)

    fun verifyTusPostHeader(tusResumable: String, uploadLength: Long, maxLength: Long)

    fun verifyTusPatchHeader(tusResumable: String, contentType: String)

    fun getPersistentFiles(owner: UUID?, pageable: Pageable): Page<File>
    fun getPersistentFile(id: UUID): File

    fun getPreviewStream(fileId: UUID): InputStream?

    fun updatePersistentFileMetadata(id: UUID, updateDTO: FileUpdateDTO): FileMetadataDTO

    fun getFileHistory(fileId: UUID): List<FileVersionDTO>
    fun getSpecificVersionStream(fileId: UUID, version: Int): Pair<FileVersion, InputStream>

    fun rollbackToPreviousVersion(fileId: UUID)

    fun saveAiMetadata(fileId: UUID, versionId: Int, aiData: AiResultDTO)
    fun setAiMetadataFailure(fileId: UUID, versionId: Int)
    fun askQuestionToAi(userId: UUID, question: String, specificFileIds: List<String>?): String

    fun getAllAllowedFileIds(userId: UUID): List<UUID>

    fun triggerAiRetry(fileId: UUID)

    fun checkWritePermissionString(file: File): String
}