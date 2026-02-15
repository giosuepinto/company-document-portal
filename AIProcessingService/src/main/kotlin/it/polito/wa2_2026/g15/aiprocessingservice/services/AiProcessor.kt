package it.polito.wa2_2026.g15.aiprocessingservice.services

import it.polito.wa2_2026.g15.aiprocessingservice.dto.AiResultDTO
import it.polito.wa2_2026.g15.aiprocessingservice.dto.FileUploadEvent
import it.polito.wa2_2026.g15.aiprocessingservice.dto.RagQueryRequestDTO
import org.slf4j.LoggerFactory
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.prompt.PromptTemplate
import org.springframework.ai.converter.BeanOutputConverter
import org.springframework.ai.document.Document
import org.springframework.ai.transformer.splitter.TokenTextSplitter
import org.springframework.ai.vectorstore.SearchRequest
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import java.nio.charset.StandardCharsets
import java.time.Instant

@Service
class AiProcessor(
    private val chatClientBuilder: ChatClient.Builder,
    @Value("\${app.fileservice.url}") private val fileServiceUrl: String,
    private val restClient: RestClient,
    private val vectorStore: VectorStore, // manages storing and metedata
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

            val textContent = String(contentBytes, StandardCharsets.UTF_8)
            logger.info("Text extracted: ${textContent.length} characters")

            // embedding (step 7)
            try {
                logger.info("Chunking text...")
                // Dividiamo il testo in blocchi di circa 800 token con un piccolo overlap
                // per non perdere il senso tra un blocco e l'altro
                val splitter = TokenTextSplitter(800, 100, 5, 10000, true)
                val initialDocument = Document(textContent)
                val chunks: List<Document> = splitter.split(initialDocument)

                logger.info("Generating embeddings and storing in DB...")
                // Trasformiamo i chunk in oggetti Document con i metadati necessari ..
                val documentsToStore = chunks.mapIndexed { index, doc ->
                    Document(
                        doc.formattedContent,
                        mapOf(
                            "fileId" to event.fileId.toString(),
                            "fileVersionId" to event.fileVersionId.toString(),
                            "filename" to event.filename,
                            "contentType" to event.contentType,
                            "ownerId" to event.ownerId.toString(),
                            "uploadTimestamp" to Instant.now().toString(),
                            "chunkIndex" to index,
                            "isFirstChunk" to (index == 0) // Utile per dare priorità agli incipit
                        )
                    )
                }
                logger.info("Embeddings: {}", documentsToStore)

                // vectorStore.add() invia i dati al modello di embedding (Gemini)
                // e salva automaticamente i vettori nel database filesdb
                vectorStore.add(documentsToStore)
                logger.info("Successfully stored ${documentsToStore.size} chunks in the vector store.")

            } catch (e: Exception) {
                logger.error("Failed to process vector storage for file ${event.fileId}", e)
                // Non blocchiamo il resto del processo se il vector store fallisce
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

        // PROMPT SICURO: Usiamo un placeholder {text_content} invece di mettere il testo qui dentro
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

        val response = chatClient.prompt()
            .user { u ->
                u.text(promptUser)
                    .param("text_content", text.take(15000)) // Passiamo il testo come parametro sicuro!
                    .param("format", outputConverter.format)
            }
            .call()
            .content()

        return outputConverter.convert(response!!)
            ?: AiResultDTO("No result", emptyList(), "PUBLIC")
    }

    fun generateAnswer(query: RagQueryRequestDTO): String {
        val chatClient = chatClientBuilder.build()

        // 1. RETRIEVAL (Recupero vettori simili ma FILTRATI)
        // Usiamo i metadati "fileId" che abbiamo salvato prima!

        val idsList = query.filterFileIds.joinToString(",") { "'$it'" }
        val searchRequest = SearchRequest.builder().query(query.question)
            .topK(5)
            .filterExpression("fileId in [$idsList]")
            .build()

        val similarDocuments = vectorStore.similaritySearch(searchRequest)
        logger.info("Similar documents: {}", similarDocuments)

        if (similarDocuments.isNullOrEmpty()) {
            return "No info found to data you have access to."
        }

        // 2. COMBINA I DOCUMENTI (Context)
        val context = similarDocuments.joinToString("\n\n") { doc ->
            val meta = doc.metadata
            """
            [DOCUMENT: ${meta["filename"]}]
            Type: ${meta["contentType"]} | Date: ${meta["uploadTimestamp"]} | Owner: ${meta["ownerId"]}
            CONTENT:
            ${doc.text}
            """
        }
        logger.info("Context: {}", context)

        // 3. GENERAZIONE (Augmented Generation)
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

        // 4. CHIAMATA AL LLM (Gemini/OpenAI)
        val response = chatClient.prompt(prompt).call().content()

        return response ?: "No result."
    }

}