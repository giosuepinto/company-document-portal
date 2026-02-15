package it.polito.wa2_2026.g15.aiprocessingservice.controllers

import it.polito.wa2_2026.g15.aiprocessingservice.dto.RagQueryRequestDTO
import it.polito.wa2_2026.g15.aiprocessingservice.services.AiProcessor
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/ai")
class AiRagController(
    private val aiProcessor: AiProcessor
) {

    private val logger = LoggerFactory.getLogger(AiRagController::class.java)

    @PostMapping("/query")
    fun performRagQuery(@RequestBody request: RagQueryRequestDTO): ResponseEntity<AiResultDTO> {
        logger.info("RAG: Request to ask theLLM for question {}", request.question)
        // 1. Il FileService ha già fatto i controlli di sicurezza.
        // Qui ci fidiamo: cerchiamo solo negli ID che ci ha passato.

        val answer = aiProcessor.generateAnswer(request)
        logger.info("RAG: Answer for question {}", request.question)

        // Puoi ritornare una stringa semplice o un DTO più complesso
        return ResponseEntity.ok(AiResultDTO(answer))
    }
}

// Simple wrapper
data class AiResultDTO(val answer: String)