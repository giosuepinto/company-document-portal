package it.polito.wa2_2026.g15.aiprocessingservice.integration

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.test.context.TestPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName

@SpringBootTest
@Testcontainers // 1. Abilita Docker
@EmbeddedKafka(partitions = 1, topics = ["file-upload.completed"]) // 2. Kafka Finto
@TestPropertySource(properties = [
    "spring.ai.vectorstore.pgvector.initialize-schema=false", // Non stressare il DB all'avvio
    "spring.kafka.consumer.auto-offset-reset=earliest"
])
class AiProcessingServiceApplicationTests {

    // 3. Forniamo un Database vero tramite Docker, altrimenti contextLoads fallisce
    // cercando di connettersi a localhost:5432
    companion object {
        @Container
        @ServiceConnection
        val pgVectorContainer = PostgreSQLContainer(DockerImageName.parse("pgvector/pgvector:pg16"))
            .withDatabaseName("testdb")
            .withUsername("postgres")
            .withPassword("postgres")
    }

    @Test
    fun contextLoads() {
        // Se arriva qui senza errori, significa che l'App si configura correttamente.
        // È il test più semplice ma fondamentale.
    }

}