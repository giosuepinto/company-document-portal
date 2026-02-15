package it.polito.wa2.lab03.userdetailservice.entities

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import org.springframework.data.util.ProxyUtils
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "users")
class User {
    @Id
    var id: UUID? = null

    @Column(nullable = false)
    var fullName: String = ""

    @Column(nullable = false, unique = true)
    var email: String = ""

    var organization: String = ""

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var role: Role = Role.STAFF

    var department: String? = null
    var storageQuota: Long? = null
    var notes: String? = null

    @CreationTimestamp
    @Column(updatable = false)
    var createdAt: Instant? = null

    @UpdateTimestamp
    var updatedAt: Instant? = null

    // --- JPA Identity Methods ---

    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (other === this) return true
        if (javaClass != ProxyUtils.getUserClass(other)) return false
        other as User
        return if (null == this.id) false else this.id == other.id
    }

    override fun hashCode(): Int {
        return 31 // Constant hash code for consistent behavior across JPA state transitions
    }

    override fun toString(): String {
        return "User(id=$id, email='$email', role=$role)"
    }
}