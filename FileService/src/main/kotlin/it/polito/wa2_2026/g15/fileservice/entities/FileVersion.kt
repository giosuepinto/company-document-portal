package it.polito.wa2_2026.g15.fileservice.entities

import jakarta.persistence.*
import java.time.Instant

@Entity
class FileVersion {

    @EmbeddedId
    var id: FileVersionId? = null

    @MapsId("fileId")
    @ManyToOne(fetch = FetchType.LAZY)
    var file: File? = null

    @Column(nullable = false)
    var size: Long = 0L

    var checksum: String? = null

    @Column(nullable = false)
    var contentType: String = "application/octet-stream"

    @Column(nullable = false)
    var filename: String = ""

    @Column(nullable = false)
    var updatedAt: Instant = Instant.now()

    @Column(nullable = false)
    var minioObjectKey: String = ""

    var previewMinioKey: String? = null

    // 1. Tags: Una lista di stringhe. JPA creerà una tabella extra "file_version_tags"
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "file_version_tags",
        joinColumns = [
            JoinColumn(name = "file_id", referencedColumnName = "file_id"),
            JoinColumn(name = "version", referencedColumnName = "version")
        ]
    )
    @Column(name = "tag")
    var tags: MutableList<String> = mutableListOf()

    // 2. Summary: Può essere lungo, quindi usiamo TEXT per non avere limiti di 255 caratteri
    @Column(columnDefinition = "TEXT")
    var summary: String? = null

    // 3. Sensitivity: Una stringa semplice (o Enum) per la classificazione
    var sensitivity: String? = null

    @Enumerated(EnumType.STRING)
    @Column
    var ai_status: AiStatus = AiStatus.PENDING


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        other as FileVersion
        return id == other.id
    }

    override fun hashCode(): Int {
        return id?.hashCode() ?: 0
    }

    override fun toString(): String {
        return "FileVersion(id: $id, file: $filename)"
    }

}