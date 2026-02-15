package it.polito.wa2_2026.g15.fileservice.controllers

import it.polito.wa2_2026.g15.fileservice.exceptions.*
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler

@RestControllerAdvice
class TusExceptionHandler : ResponseEntityExceptionHandler() {

    
    private val log = LoggerFactory.getLogger(TusExceptionHandler::class.java)

    /**
     * Handles Invalid TUS Version (412).
     * Must return the supported version in the header.
     */
    @ExceptionHandler(InvalidTusVersionException::class)
    fun handleInvalidTusVersionException(e: InvalidTusVersionException): ResponseEntity<ProblemDetail> {
        val detail = e.message ?: "Invalid TUS protocol version"
        // Uso 'log' invece di 'logger'
        log.warn("TUS Handshake Error: {}", detail)

        val problem = ProblemDetail.forStatusAndDetail(HttpStatus.PRECONDITION_FAILED, detail)

        val headers = HttpHeaders()
        headers.add("Tus-Version", "1.0.0")

        return ResponseEntity(problem, headers, HttpStatus.PRECONDITION_FAILED)
    }

    /**
     * Handles Missing TUS Version Header (400).
     */
    @ExceptionHandler(MissingTusVersionException::class)
    fun handleMissingTusVersionException(e: MissingTusVersionException): ProblemDetail {
        val detail = e.message ?: "Missing Tus-Resumable header"
        log.warn("TUS Header Error: {}", detail)

        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail)
    }

    /**
     * Handles Invalid or Missing Upload-Length (400).
     */
    @ExceptionHandler(UploadLengthException::class)
    fun handleUploadLengthException(e: UploadLengthException): ProblemDetail {
        val detail = e.message ?: "Invalid Upload-Length"
        log.warn("TUS Size Error: {}", detail)

        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail)
    }

    /**
     * Handles Missing Upload-Offset (400).
     */
    @ExceptionHandler(MissingUploadOffsetException::class)
    fun handleUploadOffsetMissingException(e: MissingUploadOffsetException): ProblemDetail {
        val detail = e.message ?: "Missing Upload-Offset"
        log.warn("TUS Offset Error: {}", detail)

        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail)
    }

    /**
     * Handles Invalid Upload-Offset (409 Conflict or 400 Bad Request).
     * Occurs when client offset does not match server offset.
     */
    @ExceptionHandler(InvalidUploadOffsetException::class)
    fun handleInvalidUploadOffsetException(e: InvalidUploadOffsetException): ProblemDetail {
        val detail = e.message ?: "Invalid Offset"
        log.error("TUS Offset Mismatch: {}", detail)

        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, detail)
    }

    /**
     * Handles Upload Not Found (404).
     */
    @ExceptionHandler(UploadNotFoundException::class)
    fun handleUploadNotFoundException(e: UploadNotFoundException): ProblemDetail {
        val detail = e.message ?: "Upload session not found"
        log.warn("TUS Lookup Error: {}", detail)

        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, detail)
    }

    /**
     * Handles Upload Already Completed (409 or 400).
     * You cannot patch a completed upload.
     */
    @ExceptionHandler(UploadAlreadyCompletedException::class)
    fun handleUploadAlreadyCompletedException(e: UploadAlreadyCompletedException): ProblemDetail {
        val detail = e.message ?: "Upload already completed"
        log.warn("TUS State Error: {}", detail)

        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, detail)
    }

    /**
     * Handles Upload Gone/Deleted (410).
     */
    @ExceptionHandler(UploadDeletedException::class)
    fun handleUploadDeletedException(e: UploadDeletedException): ProblemDetail {
        val detail = e.message ?: "Upload resource gone"
        log.warn("TUS Resource Gone: {}", detail)

        return ProblemDetail.forStatusAndDetail(HttpStatus.GONE, detail)
    }

    /**
     * Handles Storage/IO Exceptions (500).
     * Logs the full stack trace as this is a system failure.
     */
    @ExceptionHandler(StorageException::class)
    fun handleStorageException(e: StorageException): ProblemDetail {
        val detail = e.message ?: "Internal storage error"
        // Qui passiamo l'eccezione 'e' come ultimo parametro, SLF4J capisce che deve stampare lo stack trace
        log.error("TUS Critical Storage Error: {}", detail, e)

        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, detail)
    }

    /**
     * Handles Payload Too Large (413).
     */
    @ExceptionHandler(PayloadTooLargeException::class)
    fun handlePayloadTooLargeException(e: PayloadTooLargeException): ProblemDetail {
        val detail = e.message ?: "File too large"
        log.warn("TUS Size Limit Exceeded: {}", detail)

        return ProblemDetail.forStatusAndDetail(HttpStatus.PAYLOAD_TOO_LARGE, detail)
    }

    /**
     * Handles Locked Resource (423).
     * Occurs when trying to download a file that is still uploading.
     */
    @ExceptionHandler(ResourceLockedException::class)
    fun handleResourceLockedException(e: ResourceLockedException): ProblemDetail {
        val detail = e.message ?: "Resource is locked"
        log.warn("TUS Concurrency Error: {}", detail)

        return ProblemDetail.forStatusAndDetail(HttpStatus.LOCKED, detail)
    }

    /**
     * Handles Invalid Content-Type (415).
     */
    @ExceptionHandler(InvalidContentTypeException::class)
    fun handleInvalidContentTypeException(e: InvalidContentTypeException): ProblemDetail {
        val detail = e.message ?: "Invalid Content-Type"
        log.warn("TUS Media Type Error: {}", detail)

        return ProblemDetail.forStatusAndDetail(HttpStatus.UNSUPPORTED_MEDIA_TYPE, detail)
    }

    /**
     * Handles Checksum Mismatch (460 - Custom TUS Code).
     */
    @ExceptionHandler(ChecksumMismatchException::class)
    fun handleChecksumMismatchException(e: ChecksumMismatchException): ResponseEntity<ProblemDetail> {
        val detail = e.message ?: "Checksum mismatch"
        log.error("TUS Integrity Error: {}", detail)

        val problem = ProblemDetail.forStatus(460)
        problem.title = "Checksum Mismatch"
        problem.detail = detail

        return ResponseEntity.status(460).body(problem)
    }

    /**
     * Generic Exception Fallback (500).
     */
    @ExceptionHandler(Exception::class)
    fun handleTUSOtherException(e: Exception): ProblemDetail {
        val detail = e.message ?: "Unknown internal error"
        log.error("TUS Unhandled Exception: {}", detail, e)

        return ProblemDetail.forStatusAndDetail(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Internal server error: $detail"
        )
    }
}