package it.polito.wa2_2026.g15.fileservice.entities

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

/**
 * Enum representing the permission level for a shared file.
 */
enum class SharePermission { READ, WRITE }

/**
 * Entity representing a file sharing record.
 * It links a File entity to a specific User ID (managed by UserDetailService).
 */
@Entity
@Table(name = "file_share")
class FileShare {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null

    // Relationship: Many shares can refer to one File
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id", nullable = false)
    var file: File? = null

    // We store only the UUID of the user. The actual user data is in UserDetailService.
    @Column(name = "shared_with_user_id", nullable = false)
    var sharedWithUserId: UUID? = null

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var permission: SharePermission = SharePermission.READ

    @Column(nullable = false)
    var createdAt: Instant = Instant.now()
}