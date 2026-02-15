package it.polito.wa2_2026.g15.fileservice.entities

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import java.io.Serializable
import java.util.*

@Embeddable
data class FileVersionId(
    @Column(name = "file_id", nullable = false)
    var fileId: UUID? = null,

    @Column(name = "version", nullable = false)
    var version: Int = 1
) : Serializable
