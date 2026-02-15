package it.polito.wa2_2026.g15.fileservice.repositories

import it.polito.wa2_2026.g15.fileservice.entities.Upload
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor // IMPORT THIS
import java.util.UUID

interface UploadRepository : JpaRepository<Upload, UUID>, JpaSpecificationExecutor<Upload> {

    fun removeById(id: UUID)

}