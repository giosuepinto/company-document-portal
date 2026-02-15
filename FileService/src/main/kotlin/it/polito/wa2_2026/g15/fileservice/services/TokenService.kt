package it.polito.wa2_2026.g15.fileservice.services

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestTemplate

@Service
class TokenService(
    @Value("\${keycloak.m2m.client-id}") private val clientId: String,
    @Value("\${keycloak.m2m.client-secret}") private val clientSecret: String,
    @Value("\${keycloak.m2m.token-uri}") private val tokenUri: String
) {
    private val restTemplate = RestTemplate()

    // In un sistema reale, dovresti cachare questo token fino alla scadenza!
    fun getAccessToken(): String {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_FORM_URLENCODED

        val body = LinkedMultiValueMap<String, String>()
        body.add("grant_type", "client_credentials")
        body.add("client_id", clientId)
        body.add("client_secret", clientSecret)

        val request = HttpEntity(body, headers)

        // Chiamata a Keycloak
        val response = restTemplate.postForEntity(tokenUri, request, Map::class.java)

        // CORREZIONE QUI SOTTO: as? String
        return response.body?.get("access_token") as? String
            ?: throw RuntimeException("Impossibile ottenere M2M token")
    }
}