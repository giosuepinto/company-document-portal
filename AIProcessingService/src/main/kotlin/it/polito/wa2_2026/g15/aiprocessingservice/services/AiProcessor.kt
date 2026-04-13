package it.polito.wa2_2026.g15.aiprocessingservice.services

import it.polito.wa2_2026.g15.aiprocessingservice.dto.AiResultDTO
import it.polito.wa2_2026.g15.aiprocessingservice.dto.FileUploadEvent
import it.polito.wa2_2026.g15.aiprocessingservice.dto.RagQueryRequestDTO
import org.slf4j.LoggerFactory
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.prompt.PromptTemplate
import org.springframework.ai.converter.BeanOutputConverter
import org.springframework.ai.document.Document
import org.springframework.ai.reader.tika.TikaDocumentReader
import org.springframework.ai.transformer.splitter.TokenTextSplitter
import org.springframework.ai.vectorstore.SearchRequest
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.ByteArrayResource
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import java.time.Instant

// IMPORT OPENTELEMETRY
import io.opentelemetry.api.OpenTelemetry

@Service
class AiProcessor(
    private val chatClientBuilder: ChatClient.Builder,
    @param:Value("\${app.fileservice.url}") private val fileServiceUrl: String,
    private val restClient: RestClient,
    private val vectorStore: VectorStore,
    private val openTelemetry: OpenTelemetry // <-- Iniezione del motore di tracciamento
) {
    private val logger = LoggerFactory.getLogger(AiProcessor::class.java)

    @KafkaListener(topics = ["\${app.kafka.topic}"], groupId = "\${spring.kafka.consumer.group-id}")
    fun processFileUpload(event: FileUploadEvent) {

        logger.info("[1/4] Kafka event received for file: ${event.fileId}")

        try {
            // 1. SCARICA IL FILE
            logger.info("[2/4] Downloading...")
            val contentBytes = restClient.get()
                .uri("$fileServiceUrl/${event.fileId}/versions/${event.fileVersionId}/download")
                .retrieve()
                .body(ByteArray::class.java)
                ?: throw RuntimeException("File is empty")

            val resource = ByteArrayResource(contentBytes)
            val reader = TikaDocumentReader(resource)
            val extractedDocuments = reader.get()
            val textContent = extractedDocuments.joinToString("\n") { it.text.toString() } // o formattedContent

            logger.info("Text extracted: ${textContent.length} characters")

            // embedding
            try {
                logger.info("Chunking text...")
                val splitter = TokenTextSplitter(800, 100, 5, 10000, true)
                val initialDocument = Document(textContent)
                val chunks: List<Document> = splitter.split(initialDocument)

                logger.info("Generating embeddings and storing in DB...")
                val documentsToStore = chunks.mapIndexed { index, doc ->
                    Document(
                        doc.text!!, // o formattedContent
                        mapOf(
                            "fileId" to event.fileId.toString(),
                            "fileVersionId" to event.fileVersionId.toString(),
                            "filename" to event.filename,
                            "contentType" to event.contentType,
                            "ownerId" to event.ownerId.toString(),
                            "uploadTimestamp" to Instant.now().toString(),
                            "chunkIndex" to index,
                            "isFirstChunk" to (index == 0)
                        )
                    )
                }

                logger.info("Successfully generated ${documentsToStore.size} chunks.")
                if (documentsToStore.isNotEmpty()) {
                    val preview = documentsToStore.first().text!!.take(300).replace("\n", " ") // o formattedContent
                    logger.info("Preview of first chunk: $preview...")
                }

                vectorStore.add(documentsToStore)
                logger.info("Successfully stored ${documentsToStore.size} chunks in the vector store.")

            } catch (e: Exception) {
                logger.error("Failed to process vector storage for file ${event.fileId}", e)
            }

            // 2. CHIAMA GEMINI
            logger.info("[3/4] Sending prompt to Gemini...")
            val aiResponse = generateAiMetadata(textContent)
            logger.info("Gemini's response: $aiResponse")

            // 3. SALVA RISULTATI
            logger.info("[4/4] Sending results to FileService...")
            restClient.post()
                .uri("$fileServiceUrl/${event.fileId}/versions/${event.fileVersionId}/ai-metadata")
                .body(aiResponse)
                .retrieve()
                .toBodilessEntity()

            logger.info("COMPLETED: ${event.fileId}")

        } catch (e: Exception) {
            logger.error("Error: ${event.fileId}", e)
            restClient.post()
                .uri("$fileServiceUrl/${event.fileId}/versions/${event.fileVersionId}/ai-failure")
                .retrieve()
                .toBodilessEntity()
        }
    }

    private fun generateAiMetadata(text: String): AiResultDTO {
        val chatClient = chatClientBuilder.build()
        val outputConverter = BeanOutputConverter(AiResultDTO::class.java)

        val promptUser = """
            Analyze the following text and provide:
            1. A concise summary.
            2. A list of 3-5 keywords (tags).
            3. Sensitivity level: "CONFIDENTIAL" if you find passwords, tokens, or sensitive credentials, otherwise "PUBLIC".
            
            TEXT TO ANALYZE:
            {text_content}
            
            FORMAT:
            {format}
        """.trimIndent()

        // --- CREAZIONE SPAN MANUALE PER LANGTRACE ---
        val tracer = openTelemetry.getTracer("ai-processor")
        val aiSpan = tracer.spanBuilder("gemini-metadata-extraction").startSpan()

        val responseText: String
        try {
            aiSpan.makeCurrent().use { _ ->
                // Eseguiamo l'inferenza all'interno di questo Scope protetto
                val chatResponse = chatClient.prompt()
                    .user { u ->
                        u.text(promptUser)
                            .param("text_content", text.take(15000))
                            .param("format", outputConverter.format)
                    }
                    .call()
                    .chatResponse()

                responseText = chatResponse?.result?.output?.text ?: ""
                val tokenUsage = chatResponse?.metadata?.usage

                // Applichiamo i metadati
                // NUOVO STANDARD OPENTELEMETRY GEN_AI
                aiSpan.setAttribute("gen_ai.system", "gemini")
                aiSpan.setAttribute("gen_ai.request.model", "gemini-2.5-flash")

                if (tokenUsage != null) {
                    aiSpan.setAttribute("gen_ai.usage.input_tokens", tokenUsage.promptTokens.toLong())
                    aiSpan.setAttribute("gen_ai.usage.output_tokens", tokenUsage.completionTokens.toLong())
                }
            }
        } finally {
            aiSpan.end() // Chiudiamo sempre lo span per inviarlo al Collector!
        }

        return outputConverter.convert(responseText)
            ?: AiResultDTO("No result", emptyList(), "PUBLIC")
    }

    fun generateAnswer(query: RagQueryRequestDTO): String {
        val chatClient = chatClientBuilder.build()

        val idsList = query.filterFileIds.joinToString(",") { "'$it'" }
        val searchRequest = SearchRequest.builder().query(query.question)
            .topK(5)
            .filterExpression("fileId in [$idsList]")
            .build()

        val similarDocuments = vectorStore.similaritySearch(searchRequest)

        logger.info("Found ${similarDocuments?.size ?: 0} similar documents.")

        if (similarDocuments.isNullOrEmpty()) {
            return "No info found to data you have access to."
        }

        val context = similarDocuments.joinToString("\n\n") { doc ->
            val meta = doc.metadata
            """
            [DOCUMENT: ${meta["filename"]}]
            Type: ${meta["contentType"]} | Date: ${meta["uploadTimestamp"]} | Owner: ${meta["ownerId"]}
            CONTENT:
            ${doc.text} // o formattedContent
            """
        }

        logger.info("Context generated with length: ${context.length} characters")

        val promptTemplate = PromptTemplate(
            """
            You are an intelligent assistant analyzing documents.
            Use ONLY the following pieces of context to answer the user's question.
            If the information is not contained within the context, strictly answer with "I don't know".
            Do not use outside knowledge.
            
            CONTEXT:
            {context}
            
            QUESTION:
            {question}
            
            ANSWER:
            """
        )

        val prompt = promptTemplate.create(
            mapOf("context" to context, "question" to query.question)
        )

        // --- CREAZIONE SPAN MANUALE PER LANGTRACE ---
        val tracer = openTelemetry.getTracer("ai-processor")
        val aiSpan = tracer.spanBuilder("gemini-rag-query").startSpan()

        val responseText: String
        try {
            aiSpan.makeCurrent().use { _ ->
                val chatResponse = chatClient.prompt(prompt).call().chatResponse()

                responseText = chatResponse?.result?.output?.text ?: "No result."
                val tokenUsage = chatResponse?.metadata?.usage

                // NUOVO STANDARD OPENTELEMETRY GEN_AI
                aiSpan.setAttribute("gen_ai.system", "gemini")
                aiSpan.setAttribute("gen_ai.request.model", "gemini-2.5-flash")

                if (tokenUsage != null) {
                    aiSpan.setAttribute("gen_ai.usage.input_tokens", tokenUsage.promptTokens.toLong())
                    aiSpan.setAttribute("gen_ai.usage.output_tokens", tokenUsage.completionTokens.toLong())
                }
            }
        } finally {
            aiSpan.end()
        }

        return responseText
    }
}