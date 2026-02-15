package it.polito.wa2_2026.g15.fileservice.services

import it.polito.wa2_2026.g15.fileservice.dto.UserDetailsDTO
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import java.util.UUID

/**
 * Integration client to communicate with the UserDetailService.
 */
@Service
class UserClient(
    @Value("\${app.user-service.url}") private val userServiceUrl: String,
    private val tokenService: TokenService
) {

    // RestClient is the modern, synchronous HTTP client introduced in Spring Boot 3.1
    private val restClient = RestClient.create()
    private val logger = LoggerFactory.getLogger(UserClient::class.java)
    /**
     * Checks if a user exists in the remote UserDetailService.
     * @param userId The UUID of the user to check.
     * @return true if the user exists (200 OK), false otherwise (404 or error).
     */
    fun userExists(userId: UUID): Boolean {
        val token = tokenService.getAccessToken()

        return try {
            restClient.get()
                .uri("$userServiceUrl/api/v1/users/$userId")
                .header("Authorization", "Bearer $token")
                .retrieve()
                .toBodilessEntity() // We only care about the status code (200 OK)
            true
        } catch (e: Exception) {
            // Returns false if 404 Not Found or if the service is unreachable
            false
        }
    }

    fun getUserDetails(userId: UUID): UserDetailsDTO? {
        // 1. Ottieni il token M2M (Service Account)
        val token = tokenService.getAccessToken()

        return try {
            restClient.get()
                .uri("$userServiceUrl/api/v1/users/$userId")
                .header("Authorization", "Bearer $token") // Autenticazione M2M
                .retrieve()
                .body(UserDetailsDTO::class.java)
        } catch (e: Exception) {
            logger.warn("Impossibile recuperare dettagli utente $userId: ${e.message}")
            null
        }
    }


    fun getUsersInDepartment(department: String): List<UUID> {
        val token = tokenService.getAccessToken()
        return try {
            restClient.get()
                .uri("$userServiceUrl/api/v1/users/ids?department=$department")
                .header("Authorization", "Bearer $token")
                .retrieve()
                .body(object : ParameterizedTypeReference<List<UUID>>() {}) ?: emptyList()
        } catch (e: Exception) {
            logger.error("Failed to fetch users for department $department: ${e.message}")
            emptyList()
        }
    }
}