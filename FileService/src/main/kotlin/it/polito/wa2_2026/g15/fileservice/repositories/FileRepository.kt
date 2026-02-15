package it.polito.wa2_2026.g15.fileservice.repositories

import it.polito.wa2_2026.g15.fileservice.entities.File
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

@Repository
interface FileRepository : JpaRepository<File, UUID> {
    fun findByOwner(owner: UUID, pageable: Pageable): Page<File>

    @Query("""
        SELECT DISTINCT f 
        FROM File f 
        LEFT JOIN f.sharingRules s 
        WHERE f.owner IN :ownerIds 
           OR s.sharedWithUserId = :currentUserId
    """)
    fun findAllVisibleFiles(
        @Param("currentUserId") currentUserId: UUID,
        @Param("ownerIds") ownerIds: List<UUID>,
        pageable: Pageable
    ): Page<File>

    // Per gli Admin: dammi tutti gli ID del sistema
    @Query("SELECT f.id FROM File f")
    fun findAllIds(): List<UUID>

    // Per gli Utenti Normali/Manager:
    //    Ritorna ID se:
    //    - L'owner è nella lista 'visibleOwners' (che include ME STESSO)
    //    - OPPURE c'è una regola di sharing per il mio userId
    @Query("""
        SELECT DISTINCT f.id 
        FROM File f 
        LEFT JOIN f.sharingRules s 
        WHERE 
          f.owner IN :visibleOwners 
          OR 
          s.sharedWithUserId = :currentUserId
    """)
    fun findAllAllowedIds(
        @Param("currentUserId") currentUserId: UUID,
        @Param("visibleOwners") visibleOwners: List<UUID>
    ): List<UUID>

}

