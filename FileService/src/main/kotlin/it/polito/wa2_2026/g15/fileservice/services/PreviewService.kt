package it.polito.wa2_2026.g15.fileservice.services

import net.coobird.thumbnailator.Thumbnails
import org.apache.pdfbox.Loader
import org.apache.pdfbox.rendering.PDFRenderer
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream
import java.io.InputStream

@Service
class PreviewService {

    private val logger = LoggerFactory.getLogger(PreviewService::class.java)
    private val PREVIEW_SIZE = 300

    fun generatePreview(inputStream: InputStream, contentType: String): ByteArray? {
        logger.debug("Generating preview for content-type: {}", contentType)
        return try {
            when {
                contentType.startsWith("image/") -> processImage(inputStream)
                contentType == "application/pdf" -> processPdf(inputStream)
                else -> null
            }
        } catch (e: Exception) {
            logger.warn("Preview generation failed for type {}: {}", contentType, e.message)
            null
        }
    }

    private fun processImage(input: InputStream): ByteArray {
        val outputStream = ByteArrayOutputStream()
        Thumbnails.of(input)
            .size(PREVIEW_SIZE, PREVIEW_SIZE)
            .outputFormat("jpg")
            .outputQuality(0.8)
            .toOutputStream(outputStream)
        return outputStream.toByteArray()
    }

    private fun processPdf(input: InputStream): ByteArray {
        // Load PDF (Note: loads into memory, be careful with large files)
        Loader.loadPDF(input.readAllBytes()).use { document ->
            val renderer = PDFRenderer(document)
            val bufferedImage = renderer.renderImageWithDPI(0, 72f) // Render first page

            val outputStream = ByteArrayOutputStream()
            Thumbnails.of(bufferedImage)
                .size(PREVIEW_SIZE, PREVIEW_SIZE)
                .outputFormat("jpg")
                .outputQuality(0.8)
                .toOutputStream(outputStream)
            return outputStream.toByteArray()
        }
    }
}