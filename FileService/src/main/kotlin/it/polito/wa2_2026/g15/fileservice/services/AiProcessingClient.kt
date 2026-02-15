package it.polito.wa2_2026.g15.fileservice.services

import it.polito.wa2_2026.g15.fileservice.dto.RagQueryRequestDTO
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient

@Service
class AiProcessingClient(
    @Value("\${app.ai-service.url}") private val aiServiceUrl: String,
    private val tokenService: TokenService // Riusiamo lo stesso servizio per il token M2M
) {
    private val restClient = RestClient.create()
    private val logger = LoggerFactory.getLogger(AiProcessingClient::class.java)

    fun queryRAG(question: String, allowedFileIds: List<String>): String {
        // 1. Recupera il token M2M (come fatto in UserClient)
        val token = tokenService.getAccessToken()

        val requestBody = RagQueryRequestDTO(question, allowedFileIds)

        return try {
            logger.info("Calling AI Service at {} for RAG query", aiServiceUrl)

            val response = restClient.post()
                .uri("$aiServiceUrl/api/v1/ai/query")
                .header("Authorization", "Bearer $token") // Autenticazione M2M
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(String::class.java)

            response ?: "No response from AI service."
        } catch (e: Exception) {
            logger.error("Failed to communicate with AI service: ${e.message}")
            "Error: could not reach the AI module."
        }
    }
}