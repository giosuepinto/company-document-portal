package it.polito.wa2_2026.g15.fileservice.entities

import jakarta.persistence.*
import java.time.Instant
import java.util.*

@Entity
class File {

    @Id
    var id: UUID? = null

    // has to be changed: linked to the user
    @Column(nullable = false)
    var owner: UUID? = null

    @Column(nullable = false)
    var createdAt: Instant = Instant.now()

    @Column(nullable = false)
    var updatedAt: Instant = Instant.now()

    @OneToMany(mappedBy = "file", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
    var sharingRules: MutableList<FileShare> = mutableListOf()

    // Versions: bidirectional mapping. LAZY to avoid loading unless needed.
    @OneToMany(mappedBy = "file", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
    var versions: MutableList<FileVersion> = mutableListOf()


    override fun equals(other: Any?): Boolean {
        return this.id == (other as File).id
    }

    override fun hashCode(): Int {
        return this.id?.hashCode() ?: 0
    }

    override fun toString(): String {
        return "File(id: $id, owner: $owner, createdAt: $createdAt, updatedAt: $updatedAt)"
    }

}