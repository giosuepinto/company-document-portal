package it.polito.wa2_2026.g15.fileservice.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import java.util.UUID
import java.time.Instant
import org.springframework.data.util.ProxyUtils

@Entity
class Upload {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null
    @Column(nullable = false)
    var filename: String = ""
    @Column(nullable = false)
    var contentType: String = ""
    @Column(nullable = false)
    var size: Long = 0
    var checksum: String? = null
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: UploadStatus = UploadStatus.PENDING
    @Column(nullable = false)
    var createdAt: Instant = Instant.now()
    var completedAt: Instant? = null
    @Column(nullable = false)
    var bucket: String = ""
    @Column(nullable = false)
    var objectKey: String = ""

    @Column(name = "related_file_id")
    var relatedFileId: UUID? = null

    @Column(name = "owner_id")
    var ownerId: UUID? = null

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var mergeMode: MergeMode = MergeMode.SERVER_SIDE
    @Column(nullable = false)
    var chunkCount: Int = 0
    @Column(nullable = false)
    var totalOffset: Long = 0
    var jsonMetadata: String = ""

    // --- JPA BEST PRACTICES: equals() and hashCode() ---

    override fun equals(other: Any?): Boolean {
        // 1. Check if the other object is null
        if (other == null) return false

        // 2. Check for reference equality (same object)
        if (other === this) return true

        // 3. Check class type (handling JPA proxies)
        if (javaClass != ProxyUtils.getUserClass(other)) return false

        // 4. Cast and check ID
        other as Upload
        return if (null == this.id) false else this.id == other.id
    }

    override fun hashCode(): Int {
        // Return a constant hash code, as recommended for JPA entities
        // when the ID is generated and can be null transiently.
        return 31
    }

    // --- END ---

}