package it.polito.wa2_2026.g15.fileservice.entities

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "outbox")
class Outbox(
    @Id
    @GeneratedValue
    var id: UUID? = null,

    // Debezium cerca "aggregateid" di default
    @Column(name = "aggregateid")
    var aggregateid: String = "",

    // Debezium cerca "aggregatetype" di default
    @Column(name = "aggregatetype")
    var aggregatetype: String = "",

    // Debezium cerca "payload" di default
    @JdbcTypeCode(SqlTypes.JSON) // <--- AGGIUNGI QUESTA RIGA
    @Column(columnDefinition = "jsonb")
    var payload: String = "",

    @Column
    var createdAt: Instant = Instant.now()
)