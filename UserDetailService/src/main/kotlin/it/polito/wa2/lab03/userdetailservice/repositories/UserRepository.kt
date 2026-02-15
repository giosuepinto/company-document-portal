package it.polito.wa2.lab03.userdetailservice.repositories

import it.polito.wa2.lab03.userdetailservice.entities.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface UserRepository : JpaRepository<User, UUID>, JpaSpecificationExecutor<User>{
    fun existsByEmail(email: String): Boolean
    fun findByEmail(email: String): Optional<User>
    fun findAllByDepartment(department: String): List<User>
}